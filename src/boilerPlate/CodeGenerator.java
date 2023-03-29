package tr.com.hvl.hmb.kpys.sample.core.boilerPlate;

import liquibase.pro.packaged.S;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class CodeGenerator {

    private final static String PACKAGE_NAME =  "tr.com.hvl.hmb.kpys.sample";
    private static String klasorDizini = "";
    private static String veriTabaniTabloAdi = "";
    private static final String velocityTemplatesPath = "core\\boilerPlate\\velocityTemplates";
    private static final String xmlTemplatesPath = "core\\boilerPlate\\xmlTemplates";
    private static String className = "";

    public static void main(String[] args) {

        Dosya dosya = new Dosya();
        List<Dosya> dosyalar = dosya.getDosyalar();

        //Bu değişken "_" kullanılabilir. düzeltmesi kod içinde mevcut
        veriTabaniTabloAdi = "test";
        className = convertTableName(veriTabaniTabloAdi);

        //Dosyalar hangi dizine kayıt edilecek ? MAX depth = 2
        klasorDizini = "aaaa\\bbb\\dd";
        createMainFolderForBoilerTamplate();
        generateLiquibaseXml();

        dosyalar.forEach( d -> {
            if(d.getKlasorAdi().equals("entity") || d.getKlasorAdi().equals("dto")){
                getDBInfoForEntityAndDTO(d); //Entity ve DTO için
            }else{
                prepareVelocity(d); // DB bağlantısı olmadan yaratılackalar için
            }
        });
    }

    /**
     *             Tek klasör ise -> klasorYapisi = "parent"
     *              Parentı varsa veya olması isteniyorsa;
     *                  klasorYapisi = "parent\\child"
     *
     *             Boilerplate içindeki tüm dosyalar ana klasörünü
     *             "son" yazılan olarak kabul ederek işlem yapacaktır.
     */
    private static void createMainFolderForBoilerTamplate(){

        //Klasör yarat
        File file = new File(getWorkingPath(klasorDizini));
        boolean result = file.mkdirs();
        if(result) {
            System.out.println( klasorDizini + " Successfully create Folder.");
        }else{
            System.out.println( klasorDizini + " Folder is already generated");
        }
    }

    private static void getDBInfoForEntityAndDTO(Dosya dosya){

        String url = "jdbc:postgresql://localhost:5432/postgres";
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, "postgres", "postgres");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + veriTabaniTabloAdi);

            ResultSetMetaData rsMetaData = rs.getMetaData();
            int count = rsMetaData.getColumnCount();

            //Auditten gelen kolonları alma
            List<String> columnNamesDontInclude = Arrays.asList("id","version","created_by","created_date","last_modified_by","last_modified_date");

            ArrayList list = new ArrayList();
            HashMap map;

            boolean typeImportBigDecimal = false;
            boolean typeImportDate = false;
            boolean nullableImport = false;
            boolean sizeImport = false;

            for(int i = 1; i<=count; i++) {
                map = new HashMap();

                if(!columnNamesDontInclude.contains(rsMetaData.getColumnName(i))){

                    map.put("columnName", rsMetaData.getColumnName(i));

                    String[] tmp = rsMetaData.getColumnClassName(i).split("\\.");
                    String type = tmp[tmp.length-1];
                    if(type.equals("BigDecimal")){
                        typeImportBigDecimal = true;
                    }
                    if(type.equals("Date")){
                        typeImportDate = true;
                    }

                    map.put("variableType", type);

                    if(rsMetaData.isNullable(i) == 1){
                        map.put("nullable", "false");
                        nullableImport = true;
                    }

                    if(rsMetaData.getPrecision(i) > 0) {
                        map.put("length", rsMetaData.getPrecision(i));
                        sizeImport = true;
                    }

                    list.add(map);
                }
            }

            //Dosyaları oluşturmaya başla
            prepareVelocityForEntityAndDTO(list, dosya,
                    typeImportBigDecimal, typeImportDate,
                    nullableImport, sizeImport);

            conn.close();
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
    }

    private static void prepareVelocity(Dosya dosya){

        VelocityContext context = new VelocityContext();
        context.put("NAME", className);
        context.put("loweCaseName", camelCase(className));
        String klasorPackage = klasorDizini.replace("\\",".");
        context.put("packageName", PACKAGE_NAME+"."+ klasorPackage);
        context.put("user", System.getProperty("user.name"));
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        context.put("dateTime", formatter.format(date));

        fileGenerator(
                startVelocityEngine(dosya.getDosyaAdi(), context,velocityTemplatesPath),
                pathHandlerForClasses(dosya));
    }

    /**
     *
     * @param list özellik listesi
     * @param dosya boilertemplate için kullanılacak dosya özelliklleri
     *                           Bu değişken "_" kullanılabilir. düzeltmesi kod içinde mevcut
     * @param typeImportBigDecimal içinde BigDecimal var mı
     * @param typeImportDate içinde Date var mı
     * @param nullableImport nullable özellik var mı
     * @param sizeImport size özelliği var mı
     */
    private static void prepareVelocityForEntityAndDTO(List list, Dosya dosya,
                                                       boolean typeImportBigDecimal, boolean typeImportDate,
                                                       boolean nullableImport, boolean sizeImport){

        //Velocity Context içine veriyi doldurma
        VelocityContext context = new VelocityContext();
        context.put("className", className);
        context.put("camelCaseName", camelCase(className));
        String klasorPackage = klasorDizini.replace("\\",".");
        context.put("packageName", PACKAGE_NAME+"."+ klasorPackage +"."+ dosya.getKlasorAdi());
        context.put("columnList", list);
        context.put("user", System.getProperty("user.name"));
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        context.put("dateTime", formatter.format(date));

        if(typeImportBigDecimal)
            context.put("typeImportBigDecimal", "import java.math.BigDecimal;");
        if(typeImportDate)
            context.put("typeImportDate", "import java.util.Date;");
        if(nullableImport)
            context.put("nullableImport", "import javax.validation.constraints.NotNull;");
        if(sizeImport)
            context.put("sizeImport", "import javax.validation.constraints.Size;");

        fileGenerator(
                startVelocityEngine(dosya.getDosyaAdi(), context, velocityTemplatesPath),
                pathHandlerForClasses(dosya));
    }

    private static Properties getVelocityTemplatePathProperties(String path){
        //Velocity Dosya templatinin yükleneceği yer
        Properties p = new Properties();
        p.setProperty("file.resource.loader.path",getWorkingPath(path));
        return p;
    }

    private static String[] pathHandlerForClasses(Dosya dosya){

        String workingPath = getWorkingPath(klasorDizini + "\\" +  dosya.getKlasorAdi());
        String generatedFileName = "";
        String dosyaAdi = capitalFirstLetter(dosya.getDosyaAdi());

        if(dosya.isNamingException()){
            generatedFileName = (dosya.isNamingException() ? "" : className) + dosyaAdi +".java";
        }else if(dosya.getDosyaAdi().equals("entity")){
            generatedFileName = capitalFirstLetter(className) +".java";
        }else{
            generatedFileName = className + dosyaAdi +".java";
        }

        return new String[]{workingPath, generatedFileName};
    }

    private static String[] pathHandlerForXml(){

        String changelogPath = System.getProperty("user.dir")+"\\src\\main\\resources\\db.changelog\\changes";
        return new String[]{changelogPath, "TemplateX-"+className+".xml"};
    }

    private static void fileGenerator(String content, String[] args){

        String workingPath = args[0];
        String generatedFileName = args[1];

        try{
            //Klasör yarat
            File file = new File(workingPath);
            boolean result = file.mkdirs();
            if(result) {
                System.out.println( workingPath + " Successfully create Folder.");
            }else{
                System.out.println( workingPath + " Folder is already generated");
            }

            FileWriter myWriter = new FileWriter(workingPath + "\\"+ generatedFileName);
            myWriter.write(content);
            myWriter.close();
            System.out.println( generatedFileName + " Successfully wrote to the file.");

        } catch (IOException e) {
            System.out.println( generatedFileName + " An error occurred.");
            e.printStackTrace();
        }
    }

    private static String getWorkingPath(String pathExtension){

        final String source = "\\src\\main\\java\\tr\\com\\hvl\\hmb\\kpys\\sample\\";
        return System.getProperty("user.dir") + source + "\\" + pathExtension;
    }

    private static String convertTableName(String str){
        String[] tmp = str.split("\\_");
        String convertedString = "";

        for (String s : tmp) {
            convertedString = convertedString.concat(capitalFirstLetter(s));
        }
        return convertedString;
    }

    private static String capitalFirstLetter(String str){
        return str.substring(0,1).toUpperCase()+str.substring(1);
    }

    private static String camelCase(String str){
        return str.substring(0,1).toLowerCase()+str.substring(1);
    }

    private static String startVelocityEngine(String velocityFileName, VelocityContext context, String path){

        //Velocity engine i baslat
        VelocityEngine ve = new VelocityEngine();
        ve.init();

        //Initialize velocity run time engine through method  init()
        Velocity.init(getVelocityTemplatePathProperties(path));
        Template t = Velocity.getTemplate(velocityFileName+".vm");

        StringWriter writer = new StringWriter();
        //merge() is a  method of the Template class.
        //The usage of merge() is  for merging  the VelocityContext class object to produce the output.
        t.merge(context, writer);

        return writer.toString();
    }

    private static void generateLiquibaseXml(){

        VelocityContext context = new VelocityContext();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        context.put("id", className + " " + formatter.format(date));
        context.put("user",System.getProperty("user.name"));
        context.put("createSQL","");

        fileGenerator(
                startVelocityEngine("liquibaseTemplate", context, xmlTemplatesPath),
                pathHandlerForXml());
    }

    private static void updateLiquibaseDbChangeLog(){

    }
}
