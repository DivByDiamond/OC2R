package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JUnitLikeTestReporter implements TestReporter {
    private final Document document;
    private final Element testSuite;
    private final Stopwatch stopwatch;
    private final File destination;

    public JUnitLikeTestReporter(File destination) throws ParserConfigurationException {
        this.destination = destination;
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        this.testSuite = this.document.createElement("testsuite");
        Element element = this.document.createElement("testsuite");
        element.appendChild(this.testSuite);
        this.document.appendChild(element);
        this.testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        this.stopwatch = Stopwatch.createStarted();
    }

    private Element createTestCase(GameTestInfo testInfo, String name) {
        Element element = this.document.createElement("testcase");
        element.setAttribute("name", name);
        element.setAttribute("classname", testInfo.getStructureName());
        element.setAttribute("time", String.valueOf((double)testInfo.getRunTime() / 1000.0));
        this.testSuite.appendChild(element);
        return element;
    }

    @Override
    public void onTestFailed(GameTestInfo testInfo) {
        String s = testInfo.getTestName();
        String s1 = testInfo.getError().getMessage();
        Element element = this.document.createElement(testInfo.isRequired() ? "failure" : "skipped");
        element.setAttribute("message", "(" + testInfo.getStructureBlockPos().toShortString() + ") " + s1);
        Element element1 = this.createTestCase(testInfo, s);
        element1.appendChild(element);
    }

    @Override
    public void onTestSuccess(GameTestInfo testInfo) {
        String s = testInfo.getTestName();
        this.createTestCase(testInfo, s);
    }

    @Override
    public void finish() {
        this.stopwatch.stop();
        this.testSuite.setAttribute("time", String.valueOf((double)this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));

        try {
            this.save(this.destination);
        } catch (TransformerException transformerexception) {
            throw new Error("Couldn't save test report", transformerexception);
        }
    }

    public void save(File destination) throws TransformerException {
        TransformerFactory transformerfactory = TransformerFactory.newInstance();
        Transformer transformer = transformerfactory.newTransformer();
        DOMSource domsource = new DOMSource(this.document);
        StreamResult streamresult = new StreamResult(destination);
        transformer.transform(domsource, streamresult);
    }
}
