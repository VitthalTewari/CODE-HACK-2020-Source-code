package p020fi.iki.elonen;

import android.support.p002v4.media.session.PlaybackStateCompat;
import com.facebook.common.statfs.StatFsHelper;
import com.facebook.common.util.UriUtil;
import com.facebook.react.modules.systeminfo.AndroidInfoHelpers;
import com.polidea.multiplatformbleadapter.utils.Constants.BluetoothState;
import com.theartofdev.edmodo.cropper.CropImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.TimeZones;
import org.apache.commons.p029io.IOUtils;

/* renamed from: fi.iki.elonen.NanoHTTPD */
public abstract class NanoHTTPD {
    /* access modifiers changed from: private */
    public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);
    private static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";
    /* access modifiers changed from: private */
    public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, 2);
    private static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";
    /* access modifiers changed from: private */
    public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, 2);
    private static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";
    /* access modifiers changed from: private */
    public static final Logger LOG = Logger.getLogger(NanoHTTPD.class.getName());
    public static final String MIME_HTML = "text/html";
    public static final String MIME_PLAINTEXT = "text/plain";
    protected static Map<String, String> MIME_TYPES = null;
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
    public static final int SOCKET_READ_TIMEOUT = 5000;
    protected AsyncRunner asyncRunner;
    /* access modifiers changed from: private */
    public final String hostname;
    /* access modifiers changed from: private */
    public final int myPort;
    /* access modifiers changed from: private */
    public volatile ServerSocket myServerSocket;
    private Thread myThread;
    private ServerSocketFactory serverSocketFactory;
    /* access modifiers changed from: private */
    public TempFileManagerFactory tempFileManagerFactory;

    /* renamed from: fi.iki.elonen.NanoHTTPD$AsyncRunner */
    public interface AsyncRunner {
        void closeAll();

        void closed(ClientHandler clientHandler);

        void exec(ClientHandler clientHandler);
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$ClientHandler */
    public class ClientHandler implements Runnable {
        private final Socket acceptSocket;
        private final InputStream inputStream;

        public ClientHandler(InputStream inputStream2, Socket socket) {
            this.inputStream = inputStream2;
            this.acceptSocket = socket;
        }

        public void close() {
            NanoHTTPD.safeClose(this.inputStream);
            NanoHTTPD.safeClose(this.acceptSocket);
        }

        public void run() {
            OutputStream outputStream;
            Exception e;
            try {
                outputStream = this.acceptSocket.getOutputStream();
                try {
                    HTTPSession hTTPSession = new HTTPSession(NanoHTTPD.this.tempFileManagerFactory.create(), this.inputStream, outputStream, this.acceptSocket.getInetAddress());
                    while (!this.acceptSocket.isClosed()) {
                        hTTPSession.execute();
                    }
                } catch (Exception e2) {
                    e = e2;
                    try {
                        if ((!(e instanceof SocketException) || !"NanoHttpd Shutdown".equals(e.getMessage())) && !(e instanceof SocketTimeoutException)) {
                            NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client broken, or an bug in the handler code", e);
                        }
                        NanoHTTPD.safeClose(outputStream);
                        NanoHTTPD.safeClose(this.inputStream);
                        NanoHTTPD.safeClose(this.acceptSocket);
                        NanoHTTPD.this.asyncRunner.closed(this);
                    } catch (Throwable th) {
                        th = th;
                        NanoHTTPD.safeClose(outputStream);
                        NanoHTTPD.safeClose(this.inputStream);
                        NanoHTTPD.safeClose(this.acceptSocket);
                        NanoHTTPD.this.asyncRunner.closed(this);
                        throw th;
                    }
                }
            } catch (Exception e3) {
                Exception exc = e3;
                outputStream = null;
                e = exc;
                NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client broken, or an bug in the handler code", e);
                NanoHTTPD.safeClose(outputStream);
                NanoHTTPD.safeClose(this.inputStream);
                NanoHTTPD.safeClose(this.acceptSocket);
                NanoHTTPD.this.asyncRunner.closed(this);
            } catch (Throwable th2) {
                Throwable th3 = th2;
                outputStream = null;
                th = th3;
                NanoHTTPD.safeClose(outputStream);
                NanoHTTPD.safeClose(this.inputStream);
                NanoHTTPD.safeClose(this.acceptSocket);
                NanoHTTPD.this.asyncRunner.closed(this);
                throw th;
            }
            NanoHTTPD.safeClose(outputStream);
            NanoHTTPD.safeClose(this.inputStream);
            NanoHTTPD.safeClose(this.acceptSocket);
            NanoHTTPD.this.asyncRunner.closed(this);
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$ContentType */
    protected static class ContentType {
        private static final String ASCII_ENCODING = "US-ASCII";
        private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, 2);
        private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";
        private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, 2);
        private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";
        private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";
        private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, 2);
        private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";
        private final String boundary;
        private final String contentType;
        private final String contentTypeHeader;
        private final String encoding;

        public ContentType(String str) {
            this.contentTypeHeader = str;
            if (str != null) {
                this.contentType = getDetailFromContentHeader(str, MIME_PATTERN, "", 1);
                this.encoding = getDetailFromContentHeader(str, CHARSET_PATTERN, null, 2);
            } else {
                this.contentType = "";
                this.encoding = "UTF-8";
            }
            if (MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(this.contentType)) {
                this.boundary = getDetailFromContentHeader(str, BOUNDARY_PATTERN, null, 2);
            } else {
                this.boundary = null;
            }
        }

        private String getDetailFromContentHeader(String str, Pattern pattern, String str2, int i) {
            Matcher matcher = pattern.matcher(str);
            return matcher.find() ? matcher.group(i) : str2;
        }

        public String getContentTypeHeader() {
            return this.contentTypeHeader;
        }

        public String getContentType() {
            return this.contentType;
        }

        public String getEncoding() {
            return this.encoding == null ? "US-ASCII" : this.encoding;
        }

        public String getBoundary() {
            return this.boundary;
        }

        public boolean isMultipart() {
            return MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(this.contentType);
        }

        public ContentType tryUTF8() {
            if (this.encoding != null) {
                return this;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(this.contentTypeHeader);
            sb.append("; charset=UTF-8");
            return new ContentType(sb.toString());
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$Cookie */
    public static class Cookie {

        /* renamed from: e */
        private final String f2127e;

        /* renamed from: n */
        private final String f2128n;

        /* renamed from: v */
        private final String f2129v;

        public static String getHTTPTime(int i) {
            Calendar instance = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeZones.GMT_ID));
            instance.add(5, i);
            return simpleDateFormat.format(instance.getTime());
        }

        public Cookie(String str, String str2) {
            this(str, str2, 30);
        }

        public Cookie(String str, String str2, int i) {
            this.f2128n = str;
            this.f2129v = str2;
            this.f2127e = getHTTPTime(i);
        }

        public Cookie(String str, String str2, String str3) {
            this.f2128n = str;
            this.f2129v = str2;
            this.f2127e = str3;
        }

        public String getHTTPHeader() {
            return String.format("%s=%s; expires=%s", new Object[]{this.f2128n, this.f2129v, this.f2127e});
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$CookieHandler */
    public class CookieHandler implements Iterable<String> {
        private final HashMap<String, String> cookies = new HashMap<>();
        private final ArrayList<Cookie> queue = new ArrayList<>();

        public CookieHandler(Map<String, String> map) {
            String str = (String) map.get("cookie");
            if (str != null) {
                for (String trim : str.split(";")) {
                    String[] split = trim.trim().split("=");
                    if (split.length == 2) {
                        this.cookies.put(split[0], split[1]);
                    }
                }
            }
        }

        public void delete(String str) {
            set(str, "-delete-", -30);
        }

        public Iterator<String> iterator() {
            return this.cookies.keySet().iterator();
        }

        public String read(String str) {
            return (String) this.cookies.get(str);
        }

        public void set(Cookie cookie) {
            this.queue.add(cookie);
        }

        public void set(String str, String str2, int i) {
            this.queue.add(new Cookie(str, str2, Cookie.getHTTPTime(i)));
        }

        public void unloadQueue(C3138Response response) {
            Iterator it = this.queue.iterator();
            while (it.hasNext()) {
                response.addHeader("Set-Cookie", ((Cookie) it.next()).getHTTPHeader());
            }
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$DefaultAsyncRunner */
    public static class DefaultAsyncRunner implements AsyncRunner {
        private long requestCount;
        private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList());

        public List<ClientHandler> getRunning() {
            return this.running;
        }

        public void closeAll() {
            Iterator it = new ArrayList(this.running).iterator();
            while (it.hasNext()) {
                ((ClientHandler) it.next()).close();
            }
        }

        public void closed(ClientHandler clientHandler) {
            this.running.remove(clientHandler);
        }

        public void exec(ClientHandler clientHandler) {
            this.requestCount++;
            Thread thread = new Thread(clientHandler);
            thread.setDaemon(true);
            StringBuilder sb = new StringBuilder();
            sb.append("NanoHttpd Request Processor (#");
            sb.append(this.requestCount);
            sb.append(")");
            thread.setName(sb.toString());
            this.running.add(clientHandler);
            thread.start();
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$DefaultServerSocketFactory */
    public static class DefaultServerSocketFactory implements ServerSocketFactory {
        public ServerSocket create() throws IOException {
            return new ServerSocket();
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$DefaultTempFile */
    public static class DefaultTempFile implements TempFile {
        private final File file;
        private final OutputStream fstream = new FileOutputStream(this.file);

        public DefaultTempFile(File file2) throws IOException {
            this.file = File.createTempFile("NanoHTTPD-", "", file2);
        }

        public void delete() throws Exception {
            NanoHTTPD.safeClose(this.fstream);
            if (!this.file.delete()) {
                StringBuilder sb = new StringBuilder();
                sb.append("could not delete temporary file: ");
                sb.append(this.file.getAbsolutePath());
                throw new Exception(sb.toString());
            }
        }

        public String getName() {
            return this.file.getAbsolutePath();
        }

        public OutputStream open() throws Exception {
            return this.fstream;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$DefaultTempFileManager */
    public static class DefaultTempFileManager implements TempFileManager {
        private final List<TempFile> tempFiles;
        private final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

        public DefaultTempFileManager() {
            if (!this.tmpdir.exists()) {
                this.tmpdir.mkdirs();
            }
            this.tempFiles = new ArrayList();
        }

        public void clear() {
            for (TempFile delete : this.tempFiles) {
                try {
                    delete.delete();
                } catch (Exception e) {
                    NanoHTTPD.LOG.log(Level.WARNING, "could not delete file ", e);
                }
            }
            this.tempFiles.clear();
        }

        public TempFile createTempFile(String str) throws Exception {
            DefaultTempFile defaultTempFile = new DefaultTempFile(this.tmpdir);
            this.tempFiles.add(defaultTempFile);
            return defaultTempFile;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$DefaultTempFileManagerFactory */
    private class DefaultTempFileManagerFactory implements TempFileManagerFactory {
        private DefaultTempFileManagerFactory() {
        }

        public TempFileManager create() {
            return new DefaultTempFileManager();
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$HTTPSession */
    protected class HTTPSession implements IHTTPSession {
        public static final int BUFSIZE = 8192;
        public static final int MAX_HEADER_SIZE = 1024;
        private static final int MEMORY_STORE_LIMIT = 1024;
        private static final int REQUEST_BUFFER_LEN = 512;
        private CookieHandler cookies;
        private Map<String, String> headers;
        private final BufferedInputStream inputStream;
        private Method method;
        private final OutputStream outputStream;
        private Map<String, List<String>> parms;
        private String protocolVersion;
        private String queryParameterString;
        private String remoteHostname;
        private String remoteIp;
        private int rlen;
        private int splitbyte;
        private final TempFileManager tempFileManager;
        private String uri;

        public HTTPSession(TempFileManager tempFileManager2, InputStream inputStream2, OutputStream outputStream2) {
            this.tempFileManager = tempFileManager2;
            this.inputStream = new BufferedInputStream(inputStream2, 8192);
            this.outputStream = outputStream2;
        }

        public HTTPSession(TempFileManager tempFileManager2, InputStream inputStream2, OutputStream outputStream2, InetAddress inetAddress) {
            this.tempFileManager = tempFileManager2;
            this.inputStream = new BufferedInputStream(inputStream2, 8192);
            this.outputStream = outputStream2;
            this.remoteIp = (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) ? "127.0.0.1" : inetAddress.getHostAddress().toString();
            this.remoteHostname = (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) ? AndroidInfoHelpers.DEVICE_LOCALHOST : inetAddress.getHostName().toString();
            this.headers = new HashMap();
        }

        private void decodeHeader(BufferedReader bufferedReader, Map<String, String> map, Map<String, List<String>> map2, Map<String, String> map3) throws ResponseException {
            String str;
            try {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    StringTokenizer stringTokenizer = new StringTokenizer(readLine);
                    if (stringTokenizer.hasMoreTokens()) {
                        map.put("method", stringTokenizer.nextToken());
                        if (stringTokenizer.hasMoreTokens()) {
                            String nextToken = stringTokenizer.nextToken();
                            int indexOf = nextToken.indexOf(63);
                            if (indexOf >= 0) {
                                decodeParms(nextToken.substring(indexOf + 1), map2);
                                str = NanoHTTPD.decodePercent(nextToken.substring(0, indexOf));
                            } else {
                                str = NanoHTTPD.decodePercent(nextToken);
                            }
                            if (stringTokenizer.hasMoreTokens()) {
                                this.protocolVersion = stringTokenizer.nextToken();
                            } else {
                                this.protocolVersion = "HTTP/1.1";
                                NanoHTTPD.LOG.log(Level.FINE, "no protocol version specified, strange. Assuming HTTP/1.1.");
                            }
                            String readLine2 = bufferedReader.readLine();
                            while (readLine2 != null && !readLine2.trim().isEmpty()) {
                                int indexOf2 = readLine2.indexOf(58);
                                if (indexOf2 >= 0) {
                                    map3.put(readLine2.substring(0, indexOf2).trim().toLowerCase(Locale.US), readLine2.substring(indexOf2 + 1).trim());
                                }
                                readLine2 = bufferedReader.readLine();
                            }
                            map.put("uri", str);
                            return;
                        }
                        throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                    }
                    throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }
            } catch (IOException e) {
                Status status = Status.INTERNAL_ERROR;
                StringBuilder sb = new StringBuilder();
                sb.append("SERVER INTERNAL ERROR: IOException: ");
                sb.append(e.getMessage());
                throw new ResponseException(status, sb.toString(), e);
            }
        }

        private void decodeMultipartFormData(ContentType contentType, ByteBuffer byteBuffer, Map<String, List<String>> map, Map<String, String> map2) throws ResponseException {
            String sb;
            ByteBuffer byteBuffer2 = byteBuffer;
            Map<String, List<String>> map3 = map;
            Map<String, String> map4 = map2;
            try {
                int[] boundaryPositions = getBoundaryPositions(byteBuffer2, contentType.getBoundary().getBytes());
                int i = 2;
                if (boundaryPositions.length >= 2) {
                    int i2 = 1024;
                    byte[] bArr = new byte[1024];
                    int i3 = 0;
                    int i4 = 0;
                    int i5 = 0;
                    while (true) {
                        int i6 = 1;
                        if (i4 < boundaryPositions.length - 1) {
                            byteBuffer2.position(boundaryPositions[i4]);
                            int remaining = byteBuffer.remaining() < i2 ? byteBuffer.remaining() : 1024;
                            byteBuffer2.get(bArr, i3, remaining);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bArr, i3, remaining), Charset.forName(contentType.getEncoding())), remaining);
                            String readLine = bufferedReader.readLine();
                            if (readLine != null && readLine.contains(contentType.getBoundary())) {
                                String readLine2 = bufferedReader.readLine();
                                String str = null;
                                int i7 = i5;
                                String str2 = null;
                                String str3 = null;
                                int i8 = 2;
                                while (readLine2 != null && readLine2.trim().length() > 0) {
                                    Matcher matcher = NanoHTTPD.CONTENT_DISPOSITION_PATTERN.matcher(readLine2);
                                    if (matcher.matches()) {
                                        Matcher matcher2 = NanoHTTPD.CONTENT_DISPOSITION_ATTRIBUTE_PATTERN.matcher(matcher.group(i));
                                        while (matcher2.find()) {
                                            String group = matcher2.group(i6);
                                            if ("name".equalsIgnoreCase(group)) {
                                                sb = matcher2.group(2);
                                            } else {
                                                if ("filename".equalsIgnoreCase(group)) {
                                                    String group2 = matcher2.group(2);
                                                    if (!group2.isEmpty()) {
                                                        if (i7 > 0) {
                                                            StringBuilder sb2 = new StringBuilder();
                                                            sb2.append(str);
                                                            int i9 = i7 + 1;
                                                            sb2.append(String.valueOf(i7));
                                                            sb = sb2.toString();
                                                            str2 = group2;
                                                            i7 = i9;
                                                        } else {
                                                            i7++;
                                                        }
                                                    }
                                                    str2 = group2;
                                                }
                                                i6 = 1;
                                            }
                                            str = sb;
                                            i6 = 1;
                                        }
                                    }
                                    Matcher matcher3 = NanoHTTPD.CONTENT_TYPE_PATTERN.matcher(readLine2);
                                    if (matcher3.matches()) {
                                        str3 = matcher3.group(2).trim();
                                    }
                                    readLine2 = bufferedReader.readLine();
                                    i8++;
                                    i = 2;
                                    i6 = 1;
                                }
                                int i10 = 0;
                                while (true) {
                                    int i11 = i8 - 1;
                                    if (i8 <= 0) {
                                        break;
                                    }
                                    i10 = scipOverNewLine(bArr, i10);
                                    i8 = i11;
                                }
                                if (i10 < remaining - 4) {
                                    int i12 = boundaryPositions[i4] + i10;
                                    i4++;
                                    int i13 = boundaryPositions[i4] - 4;
                                    byteBuffer2.position(i12);
                                    List list = (List) map3.get(str);
                                    if (list == null) {
                                        list = new ArrayList();
                                        map3.put(str, list);
                                    }
                                    if (str3 == null) {
                                        byte[] bArr2 = new byte[(i13 - i12)];
                                        byteBuffer2.get(bArr2);
                                        list.add(new String(bArr2, contentType.getEncoding()));
                                    } else {
                                        String saveTmpFile = saveTmpFile(byteBuffer2, i12, i13 - i12, str2);
                                        if (!map4.containsKey(str)) {
                                            map4.put(str, saveTmpFile);
                                        } else {
                                            int i14 = 2;
                                            while (true) {
                                                StringBuilder sb3 = new StringBuilder();
                                                sb3.append(str);
                                                sb3.append(i14);
                                                if (!map4.containsKey(sb3.toString())) {
                                                    break;
                                                }
                                                i14++;
                                            }
                                            StringBuilder sb4 = new StringBuilder();
                                            sb4.append(str);
                                            sb4.append(i14);
                                            map4.put(sb4.toString(), saveTmpFile);
                                        }
                                        list.add(str2);
                                    }
                                    i5 = i7;
                                    i2 = 1024;
                                    i = 2;
                                    i3 = 0;
                                } else {
                                    throw new ResponseException(Status.INTERNAL_ERROR, "Multipart header size exceeds MAX_HEADER_SIZE.");
                                }
                            }
                        } else {
                            return;
                        }
                    }
                    throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but chunk does not start with boundary.");
                }
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but contains less than two boundary strings.");
            } catch (ResponseException e) {
                throw e;
            } catch (Exception e2) {
                throw new ResponseException(Status.INTERNAL_ERROR, e2.toString());
            }
        }

        private int scipOverNewLine(byte[] bArr, int i) {
            while (bArr[i] != 10) {
                i++;
            }
            return i + 1;
        }

        private void decodeParms(String str, Map<String, List<String>> map) {
            String str2;
            String str3;
            if (str == null) {
                this.queryParameterString = "";
                return;
            }
            this.queryParameterString = str;
            StringTokenizer stringTokenizer = new StringTokenizer(str, "&");
            while (stringTokenizer.hasMoreTokens()) {
                String nextToken = stringTokenizer.nextToken();
                int indexOf = nextToken.indexOf(61);
                if (indexOf >= 0) {
                    str3 = NanoHTTPD.decodePercent(nextToken.substring(0, indexOf)).trim();
                    str2 = NanoHTTPD.decodePercent(nextToken.substring(indexOf + 1));
                } else {
                    str3 = NanoHTTPD.decodePercent(nextToken).trim();
                    str2 = "";
                }
                List list = (List) map.get(str3);
                if (list == null) {
                    list = new ArrayList();
                    map.put(str3, list);
                }
                list.add(str2);
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:74:0x019f, code lost:
            r0 = e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:85:0x0214, code lost:
            r0 = e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:87:0x0216, code lost:
            r0 = e;
         */
        /* JADX WARNING: Removed duplicated region for block: B:74:0x019f A[ExcHandler: ResponseException (e fi.iki.elonen.NanoHTTPD$ResponseException), Splitter:B:1:0x0003] */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x0214 A[Catch:{ SSLException -> 0x019a, IOException -> 0x0188, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, ResponseException -> 0x019f, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f, all -> 0x019c }, ExcHandler: SocketTimeoutException (e java.net.SocketTimeoutException), Splitter:B:1:0x0003] */
        /* JADX WARNING: Removed duplicated region for block: B:87:0x0216 A[Catch:{ SSLException -> 0x019a, IOException -> 0x0188, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, ResponseException -> 0x019f, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f, all -> 0x019c }, ExcHandler: SocketException (e java.net.SocketException), Splitter:B:1:0x0003] */
        /* JADX WARNING: Unknown top exception splitter block from list: {B:75:0x01a0=Splitter:B:75:0x01a0, B:80:0x01c2=Splitter:B:80:0x01c2} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void execute() throws java.io.IOException {
            /*
                r7 = this;
                r0 = 8192(0x2000, float:1.14794E-41)
                r1 = 0
                byte[] r2 = new byte[r0]     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r3 = 0
                r7.splitbyte = r3     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.rlen = r3     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.BufferedInputStream r4 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r4.mark(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.BufferedInputStream r4 = r7.inputStream     // Catch:{ SSLException -> 0x019a, IOException -> 0x0188, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, ResponseException -> 0x019f }
                int r4 = r4.read(r2, r3, r0)     // Catch:{ SSLException -> 0x019a, IOException -> 0x0188, SocketException -> 0x0216, SocketTimeoutException -> 0x0214, ResponseException -> 0x019f }
                r5 = -1
                if (r4 == r5) goto L_0x0176
            L_0x0018:
                if (r4 <= 0) goto L_0x0039
                int r5 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r5 = r5 + r4
                r7.rlen = r5     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r4 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r4 = r7.findHeaderEnd(r2, r4)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.splitbyte = r4     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r4 = r7.splitbyte     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r4 <= 0) goto L_0x002c
                goto L_0x0039
            L_0x002c:
                java.io.BufferedInputStream r4 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r5 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r6 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r6 = 8192 - r6
                int r4 = r4.read(r2, r5, r6)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                goto L_0x0018
            L_0x0039:
                int r0 = r7.splitbyte     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r4 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r0 >= r4) goto L_0x004c
                java.io.BufferedInputStream r0 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.reset()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.BufferedInputStream r0 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r4 = r7.splitbyte     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                long r4 = (long) r4     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.skip(r4)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x004c:
                java.util.HashMap r0 = new java.util.HashMap     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.<init>()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.parms = r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.lang.String> r0 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r0 != 0) goto L_0x005f
                java.util.HashMap r0 = new java.util.HashMap     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.<init>()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.headers = r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                goto L_0x0064
            L_0x005f:
                java.util.Map<java.lang.String, java.lang.String> r0 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.clear()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x0064:
                java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.InputStreamReader r4 = new java.io.InputStreamReader     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.ByteArrayInputStream r5 = new java.io.ByteArrayInputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                int r6 = r7.rlen     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r5.<init>(r2, r3, r6)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r4.<init>(r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.<init>(r4)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.HashMap r2 = new java.util.HashMap     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r2.<init>()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.util.List<java.lang.String>> r4 = r7.parms     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.lang.String> r5 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.decodeHeader(r0, r2, r4, r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r0 = r7.remoteIp     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r0 == 0) goto L_0x0097
                java.util.Map<java.lang.String, java.lang.String> r0 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r4 = "remote-addr"
                java.lang.String r5 = r7.remoteIp     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.put(r4, r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.lang.String> r0 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r4 = "http-client-ip"
                java.lang.String r5 = r7.remoteIp     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.put(r4, r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x0097:
                java.lang.String r0 = "method"
                java.lang.Object r0 = r2.get(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r0 = (java.lang.String) r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD$Method r0 = p020fi.iki.elonen.NanoHTTPD.Method.lookup(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.method = r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD$Method r0 = r7.method     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r0 == 0) goto L_0x0150
                java.lang.String r0 = "uri"
                java.lang.Object r0 = r2.get(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r0 = (java.lang.String) r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.uri = r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD$CookieHandler r0 = new fi.iki.elonen.NanoHTTPD$CookieHandler     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD r2 = p020fi.iki.elonen.NanoHTTPD.this     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.lang.String> r4 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.<init>(r4)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r7.cookies = r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.util.Map<java.lang.String, java.lang.String> r0 = r7.headers     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = "connection"
                java.lang.Object r0 = r0.get(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r0 = (java.lang.String) r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = "HTTP/1.1"
                java.lang.String r4 = r7.protocolVersion     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                boolean r2 = r2.equals(r4)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r4 = 1
                if (r2 == 0) goto L_0x00df
                if (r0 == 0) goto L_0x00dd
                java.lang.String r2 = "(?i).*close.*"
                boolean r0 = r0.matches(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r0 != 0) goto L_0x00df
            L_0x00dd:
                r0 = 1
                goto L_0x00e0
            L_0x00df:
                r0 = 0
            L_0x00e0:
                fi.iki.elonen.NanoHTTPD r2 = p020fi.iki.elonen.NanoHTTPD.this     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD$Response r2 = r2.serve(r7)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                if (r2 == 0) goto L_0x012f
                java.util.Map<java.lang.String, java.lang.String> r1 = r7.headers     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                java.lang.String r5 = "accept-encoding"
                java.lang.Object r1 = r1.get(r5)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                java.lang.String r1 = (java.lang.String) r1     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                fi.iki.elonen.NanoHTTPD$CookieHandler r5 = r7.cookies     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                r5.unloadQueue(r2)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                fi.iki.elonen.NanoHTTPD$Method r5 = r7.method     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                r2.setRequestMethod(r5)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                fi.iki.elonen.NanoHTTPD r5 = p020fi.iki.elonen.NanoHTTPD.this     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                boolean r5 = r5.useGzipWhenAccepted(r2)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                if (r5 == 0) goto L_0x010f
                if (r1 == 0) goto L_0x010f
                java.lang.String r5 = "gzip"
                boolean r1 = r1.contains(r5)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                if (r1 == 0) goto L_0x010f
                r3 = 1
            L_0x010f:
                r2.setGzipEncoding(r3)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                r2.setKeepAlive(r0)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                java.io.OutputStream r1 = r7.outputStream     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                r2.send(r1)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                if (r0 == 0) goto L_0x0127
                boolean r0 = r2.isCloseConnection()     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                if (r0 != 0) goto L_0x0127
                p020fi.iki.elonen.NanoHTTPD.safeClose(r2)
                goto L_0x01bb
            L_0x0127:
                java.net.SocketException r0 = new java.net.SocketException     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                java.lang.String r1 = "NanoHttpd Shutdown"
                r0.<init>(r1)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                throw r0     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
            L_0x012f:
                fi.iki.elonen.NanoHTTPD$ResponseException r0 = new fi.iki.elonen.NanoHTTPD$ResponseException     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                fi.iki.elonen.NanoHTTPD$Response$Status r1 = p020fi.iki.elonen.NanoHTTPD.C3138Response.Status.INTERNAL_ERROR     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                java.lang.String r3 = "SERVER INTERNAL ERROR: Serve() returned a null response."
                r0.<init>(r1, r3)     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
                throw r0     // Catch:{ SocketException -> 0x014c, SocketTimeoutException -> 0x0148, SSLException -> 0x0144, IOException -> 0x0140, ResponseException -> 0x013d, all -> 0x0139 }
            L_0x0139:
                r0 = move-exception
                r1 = r2
                goto L_0x0218
            L_0x013d:
                r0 = move-exception
                r1 = r2
                goto L_0x01a0
            L_0x0140:
                r0 = move-exception
                r1 = r2
                goto L_0x01c2
            L_0x0144:
                r0 = move-exception
                r1 = r2
                goto L_0x01eb
            L_0x0148:
                r0 = move-exception
                r1 = r2
                goto L_0x0215
            L_0x014c:
                r0 = move-exception
                r1 = r2
                goto L_0x0217
            L_0x0150:
                fi.iki.elonen.NanoHTTPD$ResponseException r0 = new fi.iki.elonen.NanoHTTPD$ResponseException     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                fi.iki.elonen.NanoHTTPD$Response$Status r3 = p020fi.iki.elonen.NanoHTTPD.C3138Response.Status.BAD_REQUEST     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r4.<init>()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r5 = "BAD REQUEST: Syntax error. HTTP verb "
                r4.append(r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r5 = "method"
                java.lang.Object r2 = r2.get(r5)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = (java.lang.String) r2     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r4.append(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = " unhandled."
                r4.append(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = r4.toString()     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                r0.<init>(r3, r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                throw r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x0176:
                java.io.BufferedInputStream r0 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.OutputStream r0 = r7.outputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.net.SocketException r0 = new java.net.SocketException     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = "NanoHttpd Shutdown"
                r0.<init>(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                throw r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x0188:
                java.io.BufferedInputStream r0 = r7.inputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.io.OutputStream r0 = r7.outputStream     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.net.SocketException r0 = new java.net.SocketException     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                java.lang.String r2 = "NanoHttpd Shutdown"
                r0.<init>(r2)     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
                throw r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x019a:
                r0 = move-exception
                throw r0     // Catch:{ SocketException -> 0x0216, SocketTimeoutException -> 0x0214, SSLException -> 0x01ea, IOException -> 0x01c1, ResponseException -> 0x019f }
            L_0x019c:
                r0 = move-exception
                goto L_0x0218
            L_0x019f:
                r0 = move-exception
            L_0x01a0:
                fi.iki.elonen.NanoHTTPD$Response$Status r2 = r0.getStatus()     // Catch:{ all -> 0x019c }
                java.lang.String r3 = "text/plain"
                java.lang.String r0 = r0.getMessage()     // Catch:{ all -> 0x019c }
                fi.iki.elonen.NanoHTTPD$Response r0 = p020fi.iki.elonen.NanoHTTPD.newFixedLengthResponse(r2, r3, r0)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r2 = r7.outputStream     // Catch:{ all -> 0x019c }
                r0.send(r2)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r0 = r7.outputStream     // Catch:{ all -> 0x019c }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ all -> 0x019c }
            L_0x01b8:
                p020fi.iki.elonen.NanoHTTPD.safeClose(r1)
            L_0x01bb:
                fi.iki.elonen.NanoHTTPD$TempFileManager r0 = r7.tempFileManager
                r0.clear()
                goto L_0x0213
            L_0x01c1:
                r0 = move-exception
            L_0x01c2:
                fi.iki.elonen.NanoHTTPD$Response$Status r2 = p020fi.iki.elonen.NanoHTTPD.C3138Response.Status.INTERNAL_ERROR     // Catch:{ all -> 0x019c }
                java.lang.String r3 = "text/plain"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x019c }
                r4.<init>()     // Catch:{ all -> 0x019c }
                java.lang.String r5 = "SERVER INTERNAL ERROR: IOException: "
                r4.append(r5)     // Catch:{ all -> 0x019c }
                java.lang.String r0 = r0.getMessage()     // Catch:{ all -> 0x019c }
                r4.append(r0)     // Catch:{ all -> 0x019c }
                java.lang.String r0 = r4.toString()     // Catch:{ all -> 0x019c }
                fi.iki.elonen.NanoHTTPD$Response r0 = p020fi.iki.elonen.NanoHTTPD.newFixedLengthResponse(r2, r3, r0)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r2 = r7.outputStream     // Catch:{ all -> 0x019c }
                r0.send(r2)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r0 = r7.outputStream     // Catch:{ all -> 0x019c }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ all -> 0x019c }
                goto L_0x01b8
            L_0x01ea:
                r0 = move-exception
            L_0x01eb:
                fi.iki.elonen.NanoHTTPD$Response$Status r2 = p020fi.iki.elonen.NanoHTTPD.C3138Response.Status.INTERNAL_ERROR     // Catch:{ all -> 0x019c }
                java.lang.String r3 = "text/plain"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x019c }
                r4.<init>()     // Catch:{ all -> 0x019c }
                java.lang.String r5 = "SSL PROTOCOL FAILURE: "
                r4.append(r5)     // Catch:{ all -> 0x019c }
                java.lang.String r0 = r0.getMessage()     // Catch:{ all -> 0x019c }
                r4.append(r0)     // Catch:{ all -> 0x019c }
                java.lang.String r0 = r4.toString()     // Catch:{ all -> 0x019c }
                fi.iki.elonen.NanoHTTPD$Response r0 = p020fi.iki.elonen.NanoHTTPD.newFixedLengthResponse(r2, r3, r0)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r2 = r7.outputStream     // Catch:{ all -> 0x019c }
                r0.send(r2)     // Catch:{ all -> 0x019c }
                java.io.OutputStream r0 = r7.outputStream     // Catch:{ all -> 0x019c }
                p020fi.iki.elonen.NanoHTTPD.safeClose(r0)     // Catch:{ all -> 0x019c }
                goto L_0x01b8
            L_0x0213:
                return
            L_0x0214:
                r0 = move-exception
            L_0x0215:
                throw r0     // Catch:{ all -> 0x019c }
            L_0x0216:
                r0 = move-exception
            L_0x0217:
                throw r0     // Catch:{ all -> 0x019c }
            L_0x0218:
                p020fi.iki.elonen.NanoHTTPD.safeClose(r1)
                fi.iki.elonen.NanoHTTPD$TempFileManager r1 = r7.tempFileManager
                r1.clear()
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: p020fi.iki.elonen.NanoHTTPD.HTTPSession.execute():void");
        }

        private int findHeaderEnd(byte[] bArr, int i) {
            int i2 = 0;
            while (true) {
                int i3 = i2 + 1;
                if (i3 >= i) {
                    return 0;
                }
                if (bArr[i2] == 13 && bArr[i3] == 10) {
                    int i4 = i2 + 3;
                    if (i4 < i && bArr[i2 + 2] == 13 && bArr[i4] == 10) {
                        return i2 + 4;
                    }
                }
                if (bArr[i2] == 10 && bArr[i3] == 10) {
                    return i2 + 2;
                }
                i2 = i3;
            }
        }

        private int[] getBoundaryPositions(ByteBuffer byteBuffer, byte[] bArr) {
            int[] iArr = new int[0];
            if (byteBuffer.remaining() < bArr.length) {
                return iArr;
            }
            byte[] bArr2 = new byte[(bArr.length + 4096)];
            int remaining = byteBuffer.remaining() < bArr2.length ? byteBuffer.remaining() : bArr2.length;
            byteBuffer.get(bArr2, 0, remaining);
            int length = remaining - bArr.length;
            int[] iArr2 = iArr;
            int i = 0;
            while (true) {
                int[] iArr3 = iArr2;
                int i2 = 0;
                while (i2 < length) {
                    int[] iArr4 = iArr3;
                    int i3 = 0;
                    while (i3 < bArr.length && bArr2[i2 + i3] == bArr[i3]) {
                        if (i3 == bArr.length - 1) {
                            int[] iArr5 = new int[(iArr4.length + 1)];
                            System.arraycopy(iArr4, 0, iArr5, 0, iArr4.length);
                            iArr5[iArr4.length] = i + i2;
                            iArr4 = iArr5;
                        }
                        i3++;
                    }
                    i2++;
                    iArr3 = iArr4;
                }
                i += length;
                System.arraycopy(bArr2, bArr2.length - bArr.length, bArr2, 0, bArr.length);
                length = bArr2.length - bArr.length;
                if (byteBuffer.remaining() < length) {
                    length = byteBuffer.remaining();
                }
                byteBuffer.get(bArr2, bArr.length, length);
                if (length <= 0) {
                    return iArr3;
                }
                iArr2 = iArr3;
            }
        }

        public CookieHandler getCookies() {
            return this.cookies;
        }

        public final Map<String, String> getHeaders() {
            return this.headers;
        }

        public final InputStream getInputStream() {
            return this.inputStream;
        }

        public final Method getMethod() {
            return this.method;
        }

        @Deprecated
        public final Map<String, String> getParms() {
            HashMap hashMap = new HashMap();
            for (String str : this.parms.keySet()) {
                hashMap.put(str, ((List) this.parms.get(str)).get(0));
            }
            return hashMap;
        }

        public final Map<String, List<String>> getParameters() {
            return this.parms;
        }

        public String getQueryParameterString() {
            return this.queryParameterString;
        }

        private RandomAccessFile getTmpBucket() {
            try {
                return new RandomAccessFile(this.tempFileManager.createTempFile(null).getName(), "rw");
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        public final String getUri() {
            return this.uri;
        }

        public long getBodySize() {
            if (this.headers.containsKey("content-length")) {
                return Long.parseLong((String) this.headers.get("content-length"));
            }
            if (this.splitbyte < this.rlen) {
                return (long) (this.rlen - this.splitbyte);
            }
            return 0;
        }

        public void parseBody(Map<String, String> map) throws IOException, ResponseException {
            Object obj;
            DataOutput dataOutput;
            ByteArrayOutputStream byteArrayOutputStream;
            RandomAccessFile randomAccessFile;
            ByteBuffer byteBuffer;
            Map<String, String> map2 = map;
            try {
                long bodySize = getBodySize();
                if (bodySize < 1024) {
                    ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                    dataOutput = new DataOutputStream(byteArrayOutputStream2);
                    byteArrayOutputStream = byteArrayOutputStream2;
                    randomAccessFile = 0;
                } else {
                    RandomAccessFile tmpBucket = getTmpBucket();
                    byteArrayOutputStream = null;
                    dataOutput = tmpBucket;
                    randomAccessFile = tmpBucket;
                }
                try {
                    byte[] bArr = new byte[512];
                    while (this.rlen >= 0 && bodySize > 0) {
                        this.rlen = this.inputStream.read(bArr, 0, (int) Math.min(bodySize, 512));
                        bodySize -= (long) this.rlen;
                        if (this.rlen > 0) {
                            dataOutput.write(bArr, 0, this.rlen);
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        byteBuffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
                    } else {
                        byteBuffer = randomAccessFile.getChannel().map(MapMode.READ_ONLY, 0, randomAccessFile.length());
                        randomAccessFile.seek(0);
                    }
                    if (Method.POST.equals(this.method)) {
                        ContentType contentType = new ContentType((String) this.headers.get("content-type"));
                        if (!contentType.isMultipart()) {
                            byte[] bArr2 = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bArr2);
                            String trim = new String(bArr2, contentType.getEncoding()).trim();
                            if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType.getContentType())) {
                                decodeParms(trim, this.parms);
                            } else if (trim.length() != 0) {
                                map2.put("postData", trim);
                            }
                        } else if (contentType.getBoundary() != null) {
                            decodeMultipartFormData(contentType, byteBuffer, this.parms, map2);
                        } else {
                            throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
                        }
                    } else if (Method.PUT.equals(this.method)) {
                        map2.put(UriUtil.LOCAL_CONTENT_SCHEME, saveTmpFile(byteBuffer, 0, byteBuffer.limit(), null));
                    }
                    NanoHTTPD.safeClose(randomAccessFile);
                } catch (Throwable th) {
                    th = th;
                    obj = randomAccessFile;
                    NanoHTTPD.safeClose(obj);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                obj = 0;
                NanoHTTPD.safeClose(obj);
                throw th;
            }
        }

        private String saveTmpFile(ByteBuffer byteBuffer, int i, int i2, String str) {
            String str2 = "";
            if (i2 > 0) {
                FileOutputStream fileOutputStream = null;
                try {
                    TempFile createTempFile = this.tempFileManager.createTempFile(str);
                    ByteBuffer duplicate = byteBuffer.duplicate();
                    FileOutputStream fileOutputStream2 = new FileOutputStream(createTempFile.getName());
                    try {
                        FileChannel channel = fileOutputStream2.getChannel();
                        duplicate.position(i).limit(i + i2);
                        channel.write(duplicate.slice());
                        str2 = createTempFile.getName();
                        NanoHTTPD.safeClose(fileOutputStream2);
                    } catch (Exception e) {
                        e = e;
                        fileOutputStream = fileOutputStream2;
                        try {
                            throw new Error(e);
                        } catch (Throwable th) {
                            th = th;
                            NanoHTTPD.safeClose(fileOutputStream);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        fileOutputStream = fileOutputStream2;
                        NanoHTTPD.safeClose(fileOutputStream);
                        throw th;
                    }
                } catch (Exception e2) {
                    e = e2;
                    throw new Error(e);
                }
            }
            return str2;
        }

        public String getRemoteIpAddress() {
            return this.remoteIp;
        }

        public String getRemoteHostName() {
            return this.remoteHostname;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$IHTTPSession */
    public interface IHTTPSession {
        void execute() throws IOException;

        CookieHandler getCookies();

        Map<String, String> getHeaders();

        InputStream getInputStream();

        Method getMethod();

        Map<String, List<String>> getParameters();

        @Deprecated
        Map<String, String> getParms();

        String getQueryParameterString();

        String getRemoteHostName();

        String getRemoteIpAddress();

        String getUri();

        void parseBody(Map<String, String> map) throws IOException, ResponseException;
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$Method */
    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH,
        PROPFIND,
        PROPPATCH,
        MKCOL,
        MOVE,
        COPY,
        LOCK,
        UNLOCK;

        static Method lookup(String str) {
            if (str == null) {
                return null;
            }
            try {
                return valueOf(str);
            } catch (IllegalArgumentException unused) {
                return null;
            }
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$Response */
    public static class C3138Response implements Closeable {
        private boolean chunkedTransfer;
        private long contentLength;
        private InputStream data;
        private boolean encodeAsGzip;
        private final Map<String, String> header = new HashMap<String, String>() {
            public String put(String str, String str2) {
                C3138Response.this.lowerCaseHeader.put(str == null ? str : str.toLowerCase(), str2);
                return (String) super.put(str, str2);
            }
        };
        private boolean keepAlive;
        /* access modifiers changed from: private */
        public final Map<String, String> lowerCaseHeader = new HashMap();
        private String mimeType;
        private Method requestMethod;
        private IStatus status;

        /* renamed from: fi.iki.elonen.NanoHTTPD$Response$ChunkedOutputStream */
        private static class ChunkedOutputStream extends FilterOutputStream {
            public ChunkedOutputStream(OutputStream outputStream) {
                super(outputStream);
            }

            public void write(int i) throws IOException {
                write(new byte[]{(byte) i}, 0, 1);
            }

            public void write(byte[] bArr) throws IOException {
                write(bArr, 0, bArr.length);
            }

            public void write(byte[] bArr, int i, int i2) throws IOException {
                if (i2 != 0) {
                    this.out.write(String.format("%x\r\n", new Object[]{Integer.valueOf(i2)}).getBytes());
                    this.out.write(bArr, i, i2);
                    this.out.write(IOUtils.LINE_SEPARATOR_WINDOWS.getBytes());
                }
            }

            public void finish() throws IOException {
                this.out.write("0\r\n\r\n".getBytes());
            }
        }

        /* renamed from: fi.iki.elonen.NanoHTTPD$Response$IStatus */
        public interface IStatus {
            String getDescription();

            int getRequestStatus();
        }

        /* renamed from: fi.iki.elonen.NanoHTTPD$Response$Status */
        public enum Status implements IStatus {
            SWITCH_PROTOCOL(101, "Switching Protocols"),
            OK(200, "OK"),
            CREATED(CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE, "Created"),
            ACCEPTED(202, "Accepted"),
            NO_CONTENT(CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE, "No Content"),
            PARTIAL_CONTENT(206, "Partial Content"),
            MULTI_STATUS(207, "Multi-Status"),
            REDIRECT(301, "Moved Permanently"),
            FOUND(302, "Found"),
            REDIRECT_SEE_OTHER(303, "See Other"),
            NOT_MODIFIED(304, "Not Modified"),
            TEMPORARY_REDIRECT(307, "Temporary Redirect"),
            BAD_REQUEST(StatFsHelper.DEFAULT_DISK_YELLOW_LEVEL_IN_MB, "Bad Request"),
            UNAUTHORIZED(401, BluetoothState.UNAUTHORIZED),
            FORBIDDEN(403, "Forbidden"),
            NOT_FOUND(404, "Not Found"),
            METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
            NOT_ACCEPTABLE(406, "Not Acceptable"),
            REQUEST_TIMEOUT(408, "Request Timeout"),
            CONFLICT(409, "Conflict"),
            GONE(410, "Gone"),
            LENGTH_REQUIRED(411, "Length Required"),
            PRECONDITION_FAILED(412, "Precondition Failed"),
            PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
            UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
            RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
            EXPECTATION_FAILED(417, "Expectation Failed"),
            TOO_MANY_REQUESTS(429, "Too Many Requests"),
            INTERNAL_ERROR(500, "Internal Server Error"),
            NOT_IMPLEMENTED(501, "Not Implemented"),
            SERVICE_UNAVAILABLE(503, "Service Unavailable"),
            UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported");
            
            private final String description;
            private final int requestStatus;

            private Status(int i, String str) {
                this.requestStatus = i;
                this.description = str;
            }

            public static Status lookup(int i) {
                Status[] values;
                for (Status status : values()) {
                    if (status.getRequestStatus() == i) {
                        return status;
                    }
                }
                return null;
            }

            public String getDescription() {
                StringBuilder sb = new StringBuilder();
                sb.append("");
                sb.append(this.requestStatus);
                sb.append(StringUtils.SPACE);
                sb.append(this.description);
                return sb.toString();
            }

            public int getRequestStatus() {
                return this.requestStatus;
            }
        }

        protected C3138Response(IStatus iStatus, String str, InputStream inputStream, long j) {
            this.status = iStatus;
            this.mimeType = str;
            boolean z = false;
            if (inputStream == null) {
                this.data = new ByteArrayInputStream(new byte[0]);
                this.contentLength = 0;
            } else {
                this.data = inputStream;
                this.contentLength = j;
            }
            if (this.contentLength < 0) {
                z = true;
            }
            this.chunkedTransfer = z;
            this.keepAlive = true;
        }

        public void close() throws IOException {
            if (this.data != null) {
                this.data.close();
            }
        }

        public void addHeader(String str, String str2) {
            this.header.put(str, str2);
        }

        public void closeConnection(boolean z) {
            if (z) {
                this.header.put("connection", "close");
            } else {
                this.header.remove("connection");
            }
        }

        public boolean isCloseConnection() {
            return "close".equals(getHeader("connection"));
        }

        public InputStream getData() {
            return this.data;
        }

        public String getHeader(String str) {
            return (String) this.lowerCaseHeader.get(str.toLowerCase());
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public Method getRequestMethod() {
            return this.requestMethod;
        }

        public IStatus getStatus() {
            return this.status;
        }

        public void setGzipEncoding(boolean z) {
            this.encodeAsGzip = z;
        }

        public void setKeepAlive(boolean z) {
            this.keepAlive = z;
        }

        /* access modifiers changed from: protected */
        public void send(OutputStream outputStream) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeZones.GMT_ID));
            try {
                if (this.status != null) {
                    PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, new ContentType(this.mimeType).getEncoding())), false);
                    printWriter.append("HTTP/1.1 ").append(this.status.getDescription()).append(" \r\n");
                    if (this.mimeType != null) {
                        printHeader(printWriter, "Content-Type", this.mimeType);
                    }
                    if (getHeader("date") == null) {
                        printHeader(printWriter, "Date", simpleDateFormat.format(new Date()));
                    }
                    for (Entry entry : this.header.entrySet()) {
                        printHeader(printWriter, (String) entry.getKey(), (String) entry.getValue());
                    }
                    if (getHeader("connection") == null) {
                        printHeader(printWriter, "Connection", this.keepAlive ? "keep-alive" : "close");
                    }
                    if (getHeader("content-length") != null) {
                        this.encodeAsGzip = false;
                    }
                    if (this.encodeAsGzip) {
                        printHeader(printWriter, "Content-Encoding", "gzip");
                        setChunkedTransfer(true);
                    }
                    long j = this.data != null ? this.contentLength : 0;
                    if (this.requestMethod != Method.HEAD && this.chunkedTransfer) {
                        printHeader(printWriter, "Transfer-Encoding", "chunked");
                    } else if (!this.encodeAsGzip) {
                        j = sendContentLengthHeaderIfNotAlreadyPresent(printWriter, j);
                    }
                    printWriter.append(IOUtils.LINE_SEPARATOR_WINDOWS);
                    printWriter.flush();
                    sendBodyWithCorrectTransferAndEncoding(outputStream, j);
                    outputStream.flush();
                    NanoHTTPD.safeClose(this.data);
                    return;
                }
                throw new Error("sendResponse(): Status can't be null.");
            } catch (IOException e) {
                NanoHTTPD.LOG.log(Level.SEVERE, "Could not send response to the client", e);
            }
        }

        /* access modifiers changed from: protected */
        public void printHeader(PrintWriter printWriter, String str, String str2) {
            printWriter.append(str).append(": ").append(str2).append(IOUtils.LINE_SEPARATOR_WINDOWS);
        }

        /* access modifiers changed from: protected */
        public long sendContentLengthHeaderIfNotAlreadyPresent(PrintWriter printWriter, long j) {
            String header2 = getHeader("content-length");
            if (header2 != null) {
                try {
                    j = Long.parseLong(header2);
                } catch (NumberFormatException unused) {
                    Logger access$200 = NanoHTTPD.LOG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("content-length was no number ");
                    sb.append(header2);
                    access$200.severe(sb.toString());
                }
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Content-Length: ");
            sb2.append(j);
            sb2.append(IOUtils.LINE_SEPARATOR_WINDOWS);
            printWriter.print(sb2.toString());
            return j;
        }

        private void sendBodyWithCorrectTransferAndEncoding(OutputStream outputStream, long j) throws IOException {
            if (this.requestMethod == Method.HEAD || !this.chunkedTransfer) {
                sendBodyWithCorrectEncoding(outputStream, j);
                return;
            }
            ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
            sendBodyWithCorrectEncoding(chunkedOutputStream, -1);
            chunkedOutputStream.finish();
        }

        private void sendBodyWithCorrectEncoding(OutputStream outputStream, long j) throws IOException {
            if (this.encodeAsGzip) {
                GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(outputStream);
                sendBody(gZIPOutputStream, -1);
                gZIPOutputStream.finish();
                return;
            }
            sendBody(outputStream, j);
        }

        private void sendBody(OutputStream outputStream, long j) throws IOException {
            long j2;
            byte[] bArr = new byte[((int) PlaybackStateCompat.ACTION_PREPARE)];
            boolean z = j == -1;
            while (true) {
                if (j > 0 || z) {
                    if (z) {
                        j2 = 16384;
                    } else {
                        j2 = Math.min(j, PlaybackStateCompat.ACTION_PREPARE);
                    }
                    int read = this.data.read(bArr, 0, (int) j2);
                    if (read > 0) {
                        outputStream.write(bArr, 0, read);
                        if (!z) {
                            j -= (long) read;
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        public void setChunkedTransfer(boolean z) {
            this.chunkedTransfer = z;
        }

        public void setData(InputStream inputStream) {
            this.data = inputStream;
        }

        public void setMimeType(String str) {
            this.mimeType = str;
        }

        public void setRequestMethod(Method method) {
            this.requestMethod = method;
        }

        public void setStatus(IStatus iStatus) {
            this.status = iStatus;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$ResponseException */
    public static final class ResponseException extends Exception {
        private static final long serialVersionUID = 6569838532917408380L;
        private final Status status;

        public ResponseException(Status status2, String str) {
            super(str);
            this.status = status2;
        }

        public ResponseException(Status status2, String str, Exception exc) {
            super(str, exc);
            this.status = status2;
        }

        public Status getStatus() {
            return this.status;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$SecureServerSocketFactory */
    public static class SecureServerSocketFactory implements ServerSocketFactory {
        private String[] sslProtocols;
        private SSLServerSocketFactory sslServerSocketFactory;

        public SecureServerSocketFactory(SSLServerSocketFactory sSLServerSocketFactory, String[] strArr) {
            this.sslServerSocketFactory = sSLServerSocketFactory;
            this.sslProtocols = strArr;
        }

        public ServerSocket create() throws IOException {
            SSLServerSocket sSLServerSocket = (SSLServerSocket) this.sslServerSocketFactory.createServerSocket();
            if (this.sslProtocols != null) {
                sSLServerSocket.setEnabledProtocols(this.sslProtocols);
            } else {
                sSLServerSocket.setEnabledProtocols(sSLServerSocket.getSupportedProtocols());
            }
            sSLServerSocket.setUseClientMode(false);
            sSLServerSocket.setWantClientAuth(false);
            sSLServerSocket.setNeedClientAuth(false);
            return sSLServerSocket;
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$ServerRunnable */
    public class ServerRunnable implements Runnable {
        /* access modifiers changed from: private */
        public IOException bindException;
        /* access modifiers changed from: private */
        public boolean hasBinded = false;
        private final int timeout;

        public ServerRunnable(int i) {
            this.timeout = i;
        }

        public void run() {
            try {
                NanoHTTPD.this.myServerSocket.bind(NanoHTTPD.this.hostname != null ? new InetSocketAddress(NanoHTTPD.this.hostname, NanoHTTPD.this.myPort) : new InetSocketAddress(NanoHTTPD.this.myPort));
                this.hasBinded = true;
                do {
                    try {
                        Socket accept = NanoHTTPD.this.myServerSocket.accept();
                        if (this.timeout > 0) {
                            accept.setSoTimeout(this.timeout);
                        }
                        NanoHTTPD.this.asyncRunner.exec(NanoHTTPD.this.createClientHandler(accept, accept.getInputStream()));
                    } catch (IOException e) {
                        NanoHTTPD.LOG.log(Level.FINE, "Communication with the client broken", e);
                    }
                } while (!NanoHTTPD.this.myServerSocket.isClosed());
            } catch (IOException e2) {
                this.bindException = e2;
            }
        }
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$ServerSocketFactory */
    public interface ServerSocketFactory {
        ServerSocket create() throws IOException;
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$TempFile */
    public interface TempFile {
        void delete() throws Exception;

        String getName();

        OutputStream open() throws Exception;
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$TempFileManager */
    public interface TempFileManager {
        void clear();

        TempFile createTempFile(String str) throws Exception;
    }

    /* renamed from: fi.iki.elonen.NanoHTTPD$TempFileManagerFactory */
    public interface TempFileManagerFactory {
        TempFileManager create();
    }

    public static Map<String, String> mimeTypes() {
        if (MIME_TYPES == null) {
            MIME_TYPES = new HashMap();
            loadMimeTypes(MIME_TYPES, "META-INF/nanohttpd/default-mimetypes.properties");
            loadMimeTypes(MIME_TYPES, "META-INF/nanohttpd/mimetypes.properties");
            if (MIME_TYPES.isEmpty()) {
                LOG.log(Level.WARNING, "no mime types found in the classpath! please provide mimetypes.properties");
            }
        }
        return MIME_TYPES;
    }

    private static void loadMimeTypes(Map<String, String> map, String str) {
        InputStream inputStream;
        Throwable e;
        try {
            Enumeration resources = NanoHTTPD.class.getClassLoader().getResources(str);
            while (resources.hasMoreElements()) {
                URL url = (URL) resources.nextElement();
                Properties properties = new Properties();
                try {
                    inputStream = url.openStream();
                    try {
                        properties.load(inputStream);
                    } catch (IOException e2) {
                        e = e2;
                        try {
                            Logger logger = LOG;
                            Level level = Level.SEVERE;
                            StringBuilder sb = new StringBuilder();
                            sb.append("could not load mimetypes from ");
                            sb.append(url);
                            logger.log(level, sb.toString(), e);
                            safeClose(inputStream);
                            map.putAll(properties);
                        } catch (Throwable th) {
                            th = th;
                        }
                    }
                } catch (IOException e3) {
                    Throwable th2 = e3;
                    inputStream = null;
                    e = th2;
                    Logger logger2 = LOG;
                    Level level2 = Level.SEVERE;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("could not load mimetypes from ");
                    sb2.append(url);
                    logger2.log(level2, sb2.toString(), e);
                    safeClose(inputStream);
                    map.putAll(properties);
                } catch (Throwable th3) {
                    th = th3;
                    inputStream = null;
                    safeClose(inputStream);
                    throw th;
                }
                safeClose(inputStream);
                map.putAll(properties);
            }
        } catch (IOException unused) {
            Logger logger3 = LOG;
            Level level3 = Level.INFO;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("no mime types available at ");
            sb3.append(str);
            logger3.log(level3, sb3.toString());
        }
    }

    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore keyStore, KeyManager[] keyManagerArr) throws IOException {
        try {
            TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            instance.init(keyStore);
            SSLContext instance2 = SSLContext.getInstance("TLS");
            instance2.init(keyManagerArr, instance.getTrustManagers(), null);
            return instance2.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore keyStore, KeyManagerFactory keyManagerFactory) throws IOException {
        try {
            return makeSSLSocketFactory(keyStore, keyManagerFactory.getKeyManagers());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static SSLServerSocketFactory makeSSLSocketFactory(String str, char[] cArr) throws IOException {
        try {
            KeyStore instance = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream resourceAsStream = NanoHTTPD.class.getResourceAsStream(str);
            if (resourceAsStream != null) {
                instance.load(resourceAsStream, cArr);
                KeyManagerFactory instance2 = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                instance2.init(instance, cArr);
                return makeSSLSocketFactory(instance, instance2);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to load keystore from classpath: ");
            sb.append(str);
            throw new IOException(sb.toString());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static String getMimeTypeForFile(String str) {
        int lastIndexOf = str.lastIndexOf(46);
        String str2 = lastIndexOf >= 0 ? (String) mimeTypes().get(str.substring(lastIndexOf + 1).toLowerCase()) : null;
        return str2 == null ? "application/octet-stream" : str2;
    }

    /* access modifiers changed from: private */
    public static final void safeClose(Object obj) {
        if (obj != null) {
            try {
                if (obj instanceof Closeable) {
                    ((Closeable) obj).close();
                } else if (obj instanceof Socket) {
                    ((Socket) obj).close();
                } else if (obj instanceof ServerSocket) {
                    ((ServerSocket) obj).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close");
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not close", e);
            }
        }
    }

    public NanoHTTPD(int i) {
        this(null, i);
    }

    public NanoHTTPD(String str, int i) {
        this.serverSocketFactory = new DefaultServerSocketFactory();
        this.hostname = str;
        this.myPort = i;
        setTempFileManagerFactory(new DefaultTempFileManagerFactory());
        setAsyncRunner(new DefaultAsyncRunner());
    }

    public synchronized void closeAllConnections() {
        stop();
    }

    /* access modifiers changed from: protected */
    public ClientHandler createClientHandler(Socket socket, InputStream inputStream) {
        return new ClientHandler(inputStream, socket);
    }

    /* access modifiers changed from: protected */
    public ServerRunnable createServerRunnable(int i) {
        return new ServerRunnable(i);
    }

    protected static Map<String, List<String>> decodeParameters(Map<String, String> map) {
        return decodeParameters((String) map.get(QUERY_STRING_PARAMETER));
    }

    protected static Map<String, List<String>> decodeParameters(String str) {
        HashMap hashMap = new HashMap();
        if (str != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(str, "&");
            while (stringTokenizer.hasMoreTokens()) {
                String nextToken = stringTokenizer.nextToken();
                int indexOf = nextToken.indexOf(61);
                String trim = (indexOf >= 0 ? decodePercent(nextToken.substring(0, indexOf)) : decodePercent(nextToken)).trim();
                if (!hashMap.containsKey(trim)) {
                    hashMap.put(trim, new ArrayList());
                }
                String decodePercent = indexOf >= 0 ? decodePercent(nextToken.substring(indexOf + 1)) : null;
                if (decodePercent != null) {
                    ((List) hashMap.get(trim)).add(decodePercent);
                }
            }
        }
        return hashMap;
    }

    protected static String decodePercent(String str) {
        try {
            return URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.WARNING, "Encoding not supported, ignored", e);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean useGzipWhenAccepted(C3138Response response) {
        return response.getMimeType() != null && (response.getMimeType().toLowerCase().contains("text/") || response.getMimeType().toLowerCase().contains("/json"));
    }

    public final int getListeningPort() {
        if (this.myServerSocket == null) {
            return -1;
        }
        return this.myServerSocket.getLocalPort();
    }

    public final boolean isAlive() {
        return wasStarted() && !this.myServerSocket.isClosed() && this.myThread.isAlive();
    }

    public ServerSocketFactory getServerSocketFactory() {
        return this.serverSocketFactory;
    }

    public void setServerSocketFactory(ServerSocketFactory serverSocketFactory2) {
        this.serverSocketFactory = serverSocketFactory2;
    }

    public String getHostname() {
        return this.hostname;
    }

    public TempFileManagerFactory getTempFileManagerFactory() {
        return this.tempFileManagerFactory;
    }

    public void makeSecure(SSLServerSocketFactory sSLServerSocketFactory, String[] strArr) {
        this.serverSocketFactory = new SecureServerSocketFactory(sSLServerSocketFactory, strArr);
    }

    public static C3138Response newChunkedResponse(IStatus iStatus, String str, InputStream inputStream) {
        C3138Response response = new C3138Response(iStatus, str, inputStream, -1);
        return response;
    }

    public static C3138Response newFixedLengthResponse(IStatus iStatus, String str, InputStream inputStream, long j) {
        C3138Response response = new C3138Response(iStatus, str, inputStream, j);
        return response;
    }

    public static C3138Response newFixedLengthResponse(IStatus iStatus, String str, String str2) {
        byte[] bArr;
        ContentType contentType = new ContentType(str);
        if (str2 == null) {
            return newFixedLengthResponse(iStatus, str, new ByteArrayInputStream(new byte[0]), 0);
        }
        try {
            if (!Charset.forName(contentType.getEncoding()).newEncoder().canEncode(str2)) {
                contentType = contentType.tryUTF8();
            }
            bArr = str2.getBytes(contentType.getEncoding());
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.SEVERE, "encoding problem, responding nothing", e);
            bArr = new byte[0];
        }
        return newFixedLengthResponse(iStatus, contentType.getContentTypeHeader(), new ByteArrayInputStream(bArr), (long) bArr.length);
    }

    public static C3138Response newFixedLengthResponse(String str) {
        return newFixedLengthResponse(Status.OK, MIME_HTML, str);
    }

    public C3138Response serve(IHTTPSession iHTTPSession) {
        HashMap hashMap = new HashMap();
        Method method = iHTTPSession.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                iHTTPSession.parseBody(hashMap);
            } catch (IOException e) {
                Status status = Status.INTERNAL_ERROR;
                String str = MIME_PLAINTEXT;
                StringBuilder sb = new StringBuilder();
                sb.append("SERVER INTERNAL ERROR: IOException: ");
                sb.append(e.getMessage());
                return newFixedLengthResponse(status, str, sb.toString());
            } catch (ResponseException e2) {
                return newFixedLengthResponse(e2.getStatus(), MIME_PLAINTEXT, e2.getMessage());
            }
        }
        Map parms = iHTTPSession.getParms();
        parms.put(QUERY_STRING_PARAMETER, iHTTPSession.getQueryParameterString());
        return serve(iHTTPSession.getUri(), method, iHTTPSession.getHeaders(), parms, hashMap);
    }

    @Deprecated
    public C3138Response serve(String str, Method method, Map<String, String> map, Map<String, String> map2, Map<String, String> map3) {
        return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    public void setAsyncRunner(AsyncRunner asyncRunner2) {
        this.asyncRunner = asyncRunner2;
    }

    public void setTempFileManagerFactory(TempFileManagerFactory tempFileManagerFactory2) {
        this.tempFileManagerFactory = tempFileManagerFactory2;
    }

    public void start() throws IOException {
        start(5000);
    }

    public void start(int i) throws IOException {
        start(i, true);
    }

    public void start(int i, boolean z) throws IOException {
        this.myServerSocket = getServerSocketFactory().create();
        this.myServerSocket.setReuseAddress(true);
        ServerRunnable createServerRunnable = createServerRunnable(i);
        this.myThread = new Thread(createServerRunnable);
        this.myThread.setDaemon(z);
        this.myThread.setName("NanoHttpd Main Listener");
        this.myThread.start();
        while (!createServerRunnable.hasBinded && createServerRunnable.bindException == null) {
            try {
                Thread.sleep(10);
            } catch (Throwable unused) {
            }
        }
        if (createServerRunnable.bindException != null) {
            throw createServerRunnable.bindException;
        }
    }

    public void stop() {
        try {
            safeClose(this.myServerSocket);
            this.asyncRunner.closeAll();
            if (this.myThread != null) {
                this.myThread.join();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not stop all connections", e);
        }
    }

    public final boolean wasStarted() {
        return (this.myServerSocket == null || this.myThread == null) ? false : true;
    }
}
