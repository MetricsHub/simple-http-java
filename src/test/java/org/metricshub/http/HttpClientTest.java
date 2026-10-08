package org.metricshub.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reading of the response body, against a local server
 */
class HttpClientTest {

	private static final String TEXT = "{\"name\":\"café 中 😀\",\"list\":[1,2,3]}";

	private static HttpServer server;
	private static String baseUrl;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		respond("/length", 200, "application/json", null, TEXT.getBytes(StandardCharsets.UTF_8), false);
		respond("/chunked", 200, "application/json", null, TEXT.getBytes(StandardCharsets.UTF_8), true);
		respond(
			"/latin1",
			200,
			"text/plain; charset=ISO-8859-1",
			null,
			"café".getBytes(StandardCharsets.ISO_8859_1),
			false
		);
		respond("/gzip", 200, "application/json", "gzip", gzip(TEXT.getBytes(StandardCharsets.UTF_8)), true);
		respond("/empty", 200, "application/json", null, new byte[0], false);
		respond("/error", 404, "text/plain", null, "not found".getBytes(StandardCharsets.UTF_8), false);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	private static void respond(
		String path,
		int status,
		String contentType,
		String contentEncoding,
		byte[] content,
		boolean chunked
	) {
		server.createContext(
			path,
			exchange -> {
				exchange.getResponseHeaders().add("Content-Type", contentType);
				if (contentEncoding != null) {
					exchange.getResponseHeaders().add("Content-Encoding", contentEncoding);
				}
				// Length 0 means chunked, -1 means no body
				exchange.sendResponseHeaders(status, chunked ? 0 : content.length == 0 ? -1 : content.length);
				try (OutputStream os = exchange.getResponseBody()) {
					if (content.length > 0) {
						os.write(content);
					}
				}
			}
		);
	}

	private static byte[] gzip(byte[] content) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(content);
		}
		return out.toByteArray();
	}

	private static HttpResponse get(String path) throws IOException {
		return HttpClient.sendRequest(
			baseUrl + path,
			"GET",
			null,
			null,
			null,
			null,
			0,
			null,
			null,
			null,
			null,
			null,
			10,
			null
		);
	}

	@Test
	void testBodyWithContentLength() throws IOException {
		HttpResponse response = get("/length");
		assertEquals(200, response.getStatusCode());
		assertEquals(TEXT, response.getBody());
	}

	@Test
	void testBodyChunked() throws IOException {
		assertEquals(TEXT, get("/chunked").getBody());
	}

	@Test
	void testBodyCharsetFromContentType() throws IOException {
		assertEquals("café", get("/latin1").getBody());
	}

	@Test
	void testBodyGzip() throws IOException {
		assertEquals(TEXT, get("/gzip").getBody());
	}

	@Test
	void testBodyEmpty() throws IOException {
		assertEquals("", get("/empty").getBody());
	}

	@Test
	void testBodyOfError() throws IOException {
		HttpResponse response = get("/error");
		assertEquals(404, response.getStatusCode());
		assertEquals("not found", response.getBody());
		assertEquals(response.getHeader() + "\nnot found", response.toString());
	}
}
