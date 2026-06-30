package com.example.stodo.sync;

import com.example.stodo.repository.InMemoryTaskRepository;

/**
 * Unit and integration tests for SyncServer.
 */
public class SyncServerTest {
    private static final String SYNC_PAYLOAD = "{\"protocolVersion\":1,\"lastSyncTimestamp\":0,\"clientChanges\":[]}";
    private static final int DIVISION_FACTOR_TWO = 2;
    private static final int SUBSTRING_START_INDEX = 0;
    private static final long CHUNK_DELAY_MS = 100L;

    private SyncServer server;
    private InMemoryTaskRepository repository;

    /**
     * Initializes test fixtures before each test execution.
     * Example: setUp();
     */
    @org.junit.Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        server = new SyncServer(repository);
    }

    /**
     * Cleans up resources by stopping the SyncServer.
     * Example: tearDown();
     */
    @org.junit.After
    public void tearDown() {
        server.stop();
    }

    /**
     * Verifies that the SyncServer correctly parses and processes a chunked request body.
     * Example: testSyncChunkedBody();
     */
    @org.junit.Test
    public void testSyncChunkedBody() throws Exception {
        int port = server.start();
        org.junit.Assert.assertTrue("Server failed to start", port > 0);

        try (java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
             java.io.OutputStream out = socket.getOutputStream();
             java.io.BufferedReader in = new java.io.BufferedReader(
                 new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            
            writeHttpRequestHeaders(out, SYNC_PAYLOAD.length());
            writeHttpRequestBodyChunked(out, SYNC_PAYLOAD);
            verifyHttpResponse(in);
        }
    }

    /**
     * Verifies that the SyncServer handles premature socket closure gracefully.
     * Example: testSyncPrematureClose();
     */
    @org.junit.Test
    public void testSyncPrematureClose() throws Exception {
        int port = server.start();
        org.junit.Assert.assertTrue("Server failed to start", port > 0);

        try (java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
             java.io.OutputStream out = socket.getOutputStream()) {
            
            writeHttpRequestHeaders(out, SYNC_PAYLOAD.length());
            int mid = SYNC_PAYLOAD.length() / DIVISION_FACTOR_TWO;
            String part1 = SYNC_PAYLOAD.substring(SUBSTRING_START_INDEX, mid);
            out.write(part1.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * Verifies that the SyncServer responds correctly to a ping GET request.
     * Example: testServerPing();
     */
    @org.junit.Test
    public void testServerPing() throws Exception {
        int port = server.start();
        org.junit.Assert.assertTrue("Server failed to start", port > 0);

        try (java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
             java.io.OutputStream out = socket.getOutputStream();
             java.io.BufferedReader in = new java.io.BufferedReader(
                 new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            
            String headers = "GET /api/v1/ping HTTP/1.1\r\n" +
                    "Host: 127.0.0.1\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(headers.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();

            String statusLine = in.readLine();
            org.junit.Assert.assertNotNull("Status line is null", statusLine);
            org.junit.Assert.assertTrue("Response status was not OK: " + statusLine,
                    statusLine.contains("200 OK"));
        }
    }

    private void writeHttpRequestHeaders(java.io.OutputStream out, int bodyLength) throws Exception {
        String headers = "POST /api/v1/sync HTTP/1.1\r\n" +
                "Host: 127.0.0.1\r\n" +
                "Content-Length: " + bodyLength + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(headers.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }

    private void writeHttpRequestBodyChunked(java.io.OutputStream out, String body) throws Exception {
        int mid = body.length() / DIVISION_FACTOR_TWO;
        String part1 = body.substring(SUBSTRING_START_INDEX, mid);
        String part2 = body.substring(mid);

        out.write(part1.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
        
        Thread.sleep(CHUNK_DELAY_MS);

        out.write(part2.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }

    private void verifyHttpResponse(java.io.BufferedReader in) throws Exception {
        String statusLine = in.readLine();
        org.junit.Assert.assertNotNull("Status line is null", statusLine);
        org.junit.Assert.assertTrue("Response status was not OK: " + statusLine,
                statusLine.contains("200 OK"));
        
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // Read until empty line
        }

        String responseBody = in.readLine();
        org.junit.Assert.assertNotNull("Response body is null", responseBody);
        org.junit.Assert.assertTrue("Missing protocol version in response", 
                responseBody.contains("protocolVersion"));
    }
}
