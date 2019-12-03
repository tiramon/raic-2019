package batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class BatchRunner {
	public static void main(String[] args) throws IOException, InterruptedException {
		Gson gson = new Gson();
		File workingDirectory = new File("C:\\Users\\jha\\Downloads\\aicup2019-windows\\");
		if (!workingDirectory.exists())
			throw new RuntimeException();
		String filename = "aicup2019.exe";
		File executable = new File(workingDirectory, filename);
		if (!executable.exists())
			throw new RuntimeException();
		for (int i = 0; i < 10; i++) {
			File result = new File(workingDirectory, "save/a" + i);
			ProcessBuilder builder = new ProcessBuilder(executable.getAbsolutePath(), "--batch-mode", "--config",
					"config2.json", "--save-results", "save/a" + i, "--save-replay", "replay/a" + i);

			builder.directory(workingDirectory);
			builder.inheritIO();
			Process process = builder.start();
			process.waitFor();

			String resultContent = Files.lines(result.toPath()).collect(Collectors.joining(""));
			Result resultObj = gson.fromJson(resultContent, Result.class);
			System.out.println("crash " + resultObj.players[0].crashed + "/" + resultObj.players[0].crashed + " "
					+ resultObj.results[0] + "/" + resultObj.results[1]);
		}
	}
}
