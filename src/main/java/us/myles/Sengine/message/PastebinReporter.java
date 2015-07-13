package us.myles.Sengine.message;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PastebinReporter {
	/**
	 * The possible expiration dates supported by Pastebin.
	 */
	public static enum ExpireDate {
		NEVER("N"),
		TEN_MINUTES("10M"),
		ONE_HOUR("1H"),
		ONE_DAY("1D"),
		ONE_WEEK("1W"),
		TWO_WEEKS("2W"),
		ONE_MONTH("1M");

		private final String key;

		ExpireDate(String keyValue) {
			this.key = keyValue;
		}

		@Override
		public String toString() {
			return this.key;
		}
	}

	/**
	 * The Paste, this will be the file submitted to pastebin.
	 */
	public static class Paste {
		private final Paste INSTANCE;

		private String HEADER = "";
		private char NEW_LINE = '\n';
		private List<String> TEXT = new ArrayList<String>();

		public Paste() {
			INSTANCE = this;
		}

		public Paste(String header) {
			this();
			this.HEADER = header;
		}

		public Paste(File file) {
			this(file, false);
		}

		public Paste(final File file, boolean async) {
			this();
			try {
				if (async) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							scanFile(file);
						}
					};

					new Thread(runnable).start();
				} else {
					scanFile(file);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Paste(String[] s) {
			this();
			for (String x : s) {
				appendLine(x);
			}
		}

		private void scanFile(File file) {
			try {
				String line;
				BufferedReader reader = new BufferedReader(new FileReader(file));
				while ((line = reader.readLine()) != null) {
					TEXT.add(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Paste appendLine(String string) {
			this.TEXT.add(string);
			return this.INSTANCE;
		}

		public Paste appendLine(int index, String string) {
			this.TEXT.add(index, string);
			return this.INSTANCE;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (HEADER != null) {
				builder.append(HEADER);
			}
			for (String line : TEXT) {
				builder.append(line + NEW_LINE);
			}
			return builder.toString();
		}
	}

	private final String POST_URL = "http://pastebin.com/api/api_post.php";

	private String API_KEY;

	public PastebinReporter(String apiKey) {
		this.API_KEY = apiKey;
	}

	public String post(String name, Paste paste, ExpireDate expireDate) {
		if (name == null)
			name = "";

		String report_url = "";

		try {
			URL urls = new URL(this.POST_URL);
			HttpURLConnection conn = (HttpURLConnection) urls.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestMethod("POST");
			conn.addRequestProperty("Content-type", "application/x-www-form-urlencoded");
			conn.setInstanceFollowRedirects(false);
			conn.setDoOutput(true);
			OutputStream out = conn.getOutputStream();

			byte[] data = ("api_option=paste" +
					"&api_dev_key=" + URLEncoder.encode(this.API_KEY, "utf-8") +
					"&api_paste_code=" + URLEncoder.encode(paste.toString(), "utf-8") +
					"&api_paste_private=" + URLEncoder.encode("1", "utf-8") + // 1 = unlisted, 0 = public, 2 = private (need to be logged in for that)
					"&api_paste_name=" + URLEncoder.encode(name, "utf-8") +
					"&api_paste_expire_date=" + URLEncoder.encode(expireDate.toString(), "utf-8") +
					"&api_paste_format=" + URLEncoder.encode("text", "utf-8") +
					"&api_user_key=" + URLEncoder.encode("", "utf-8")).getBytes();

			out.write(data);
			out.flush();
			out.close();

			if (conn.getResponseCode() == 200) {
				InputStream receive = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(receive));
				String line;
				StringBuffer response = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					response.append(line);
					response.append("\r\n");
				}
				reader.close();

				String result = response.toString().trim();

				if (!result.contains("http://")) {
					report_url = "Failed to post! (returned result: " + result;
				} else {
					report_url = result.trim();
				}
			} else {
				report_url = "Failed to post!";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return report_url;
	}
}