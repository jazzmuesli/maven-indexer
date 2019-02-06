package org.apache.maven.index.reader;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.index.reader.Record.EntryKey;


public class IndexCSVExporter {

	public static void main(String[] args) {

		try {
			new IndexCSVExporter().export();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void export() throws Exception {
		final File indexDir = new File("index/");
		indexDir.mkdirs();
		final File cacheDir = new File("cache/");
		cacheDir.mkdirs();

		//TODO: extract those classes from src/test into src/main or similar
		final WritableResourceHandler local = new DirectoryResourceHandler(indexDir);
		final CachingResourceHandler remote = new CachingResourceHandler(new DirectoryResourceHandler(cacheDir),
				new HttpResourceHandler(new URL("http://repo1.maven.org/maven2/.index/")));

		FileWriter fw = new FileWriter("result.txt");
		final IndexReader indexReader = new IndexReader(local, remote);
		List<EntryKey> keys = new ArrayList<EntryKey>();
		Set<String> EXCLUDED_FIELDS = new HashSet<String>(Arrays.asList("description", "sha1", "name"));
		final String delim = ";";
		for (ChunkReader chunkReader : indexReader) {
			try {
				final RecordExpander recordExpander = new RecordExpander();
				for (Map<String, String> rec : chunkReader) {
					final Record record = recordExpander.apply(rec);
					if (keys.isEmpty()) {
						keys = record.getExpanded().keySet()
								.stream()
								.filter(p -> !EXCLUDED_FIELDS.contains(p.getName()))
								.collect(Collectors.toList());
						String strKey = "type;"
								+ keys.stream().map(x -> x.getName())
								.collect(Collectors.joining(delim));
						fw.append(strKey).append("\n");
					}
					String value = keys.stream()
							.map(k -> record.getExpanded().getOrDefault(k, "NA").toString())
							.collect(Collectors.joining(delim));
					fw.append(record.getType() + ";" + value).append("\n");
				}
			} finally {
				chunkReader.close();
			}

		}

		indexReader.close();
		fw.close();
	}
}
