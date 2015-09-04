package com.carma.swagger.doclet.apidocs;

import static com.carma.swagger.doclet.apidocs.FixtureLoader.loadFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.carma.swagger.doclet.DocletOptions;
import com.carma.swagger.doclet.ObjectMapperRecorder;
import com.carma.swagger.doclet.Recorder;
import com.carma.swagger.doclet.model.ApiDeclaration;
import com.carma.swagger.doclet.parser.JaxRsAnnotationParser;
import com.sun.javadoc.RootDoc;

@SuppressWarnings("javadoc")
public class ProducesMethodsTest {

	private Recorder recorderMock;
	private DocletOptions options;

	@Before
	public void setup() {
		this.recorderMock = mock(Recorder.class);
		this.options = new DocletOptions().setRecorder(this.recorderMock).setIncludeSwaggerUi(false);
	}

	@Test
	public void testStart() throws IOException {
		final RootDoc rootDoc = RootDocLoader.fromPath("src/test/resources", "fixtures.producesmethods");
		new JaxRsAnnotationParser(this.options, rootDoc).run();

		final ApiDeclaration api = loadFixture("/fixtures/producesmethods/producesmethods.json", ApiDeclaration.class);
		verify(this.recorderMock).record(any(File.class), eq(api));
	}

	public static void main(String... args) throws IOException {
		final Recorder recorder = new ObjectMapperRecorder(null, null, null, null);
		final DocletOptions options = new DocletOptions().setRecorder(recorder).setIncludeSwaggerUi(false);
		final RootDoc rootDoc = RootDocLoader.fromPath("src/test/resources", "fixtures.producesmethods");
		new JaxRsAnnotationParser(options, rootDoc).run();
		System.out.println("done");
	}

}
