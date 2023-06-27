package com.example.postgresneo4jmigrationtool.parser.validator;

import com.jcabi.xml.XML;
import com.jcabi.xml.XSD;
import lombok.Data;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Collection;

@Data
public class XmlValidator implements Validator {

    private final XML xml;
    private final XSD xsd;
    private boolean valid = true;

    @Override
    public void validate() {
        Collection<SAXParseException> errors = xsd.validate(
                new StreamSource(new StringReader(xml.toString()))
        );
        if (!errors.isEmpty()) {
            System.out.println("VALIDATION FAILED");
            for (SAXParseException e : errors) {
                System.out.println(e.toString());
            }
            valid = false;
        } else {
            valid = true;
        }
    }

}
