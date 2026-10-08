package org.metricshub.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpResponseTest {

	@Test
	void testGetStatusCode() {
		HttpResponse r = new HttpResponse();
		r.setStatusCode(200);
		assertEquals(200, r.getStatusCode());
	}

	@Test
	void testGetHeader() {
		HttpResponse r = new HttpResponse();
		assertTrue(r.getHeader().isEmpty());
		r.appendHeader("first", "premier");
		r.appendHeader(null, null);
		r.appendHeader(null, "test");
		r.appendHeader("", "test");
		r.appendHeader("test", null);
		r.appendHeader("test", "");
		r.appendHeader("second", "1 + 1");
		assertEquals("first: premier\nsecond: 1 + 1\n", r.getHeader());
	}

	@Test
	void testGetBody() {
		HttpResponse r = new HttpResponse();
		assertTrue(r.getBody().isEmpty());
		r.appendBody("abc");
		r.appendBody("def");
		assertEquals("abcdef", r.getBody());
		r.appendBody(null);
		r.appendBody("");
		assertEquals("abcdefnull", r.getBody());

		// The body is not copied
		HttpResponse single = new HttpResponse();
		String body = "single";
		single.appendBody(body);
		assertSame(body, single.getBody());
		assertSame(single.getBody(), single.getBody());
	}

	@Test
	void testToString() {
		HttpResponse r = new HttpResponse();
		r.setStatusCode(200);
		r.appendHeader("first", "premier");
		r.appendHeader("second", "1 + 1");
		r.appendBody("abc");
		r.appendBody("def");
		assertEquals("first: premier\nsecond: 1 + 1\n\nabcdef", r.toString());
	}
}
