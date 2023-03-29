package tr.com.hvl.hmb.kpys.sample.core.boilerPlate;

import java.util.ArrayList;
import java.util.List;

public class Dosya {

    private String klasorAdi;
    private String dosyaAdi;
    private boolean namingException;

    private List<Dosya> dosyalar;

    public Dosya(String klasorAdi, String dosyaAdi) {
        this.klasorAdi = klasorAdi;
        this.dosyaAdi = dosyaAdi;
        this.namingException = false;
    }

    public Dosya(String klasorAdi, String dosyaAdi, boolean namingException) {
        this.klasorAdi = klasorAdi;
        this.dosyaAdi = dosyaAdi;
        this.namingException = namingException;
    }

    public String getKlasorAdi() {
        return klasorAdi;
    }

    public String getDosyaAdi() {
        return dosyaAdi;
    }

    public boolean isNamingException() {
        return namingException;
    }

    public Dosya() {
    }

    public List<Dosya> getDosyalar(){
        dosyalar = new ArrayList<>();
        dosyalar.add(new Dosya("controller","controller"));
        dosyalar.add(new Dosya("dto","createDTO"));
        dosyalar.add(new Dosya("dto","updateDTO"));
        dosyalar.add(new Dosya("dto","reportDTO", true));
        dosyalar.add(new Dosya("entity","entity"));
        dosyalar.add(new Dosya("mapper","createMapper"));
        dosyalar.add(new Dosya("mapper","updateMapper"));
        dosyalar.add(new Dosya("report","report"));
        dosyalar.add(new Dosya("report","excelReport"));
        dosyalar.add(new Dosya("report","pdfReport"));
        dosyalar.add(new Dosya("repository","repository"));
        dosyalar.add(new Dosya("service","service"));
        dosyalar.add(new Dosya("service","serviceImpl"));
        return dosyalar;
    }
}

