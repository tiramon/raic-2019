package batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class BatchRunner {
	public static void main(String[] args) throws IOException, InterruptedException {
		Gson gson = new Gson();
		File borJar = new File(args[0]);
		if (!borJar.exists())
			throw new RuntimeException("Bot jar does not exist");

		File workingDirectory = new File(args[1]);
		if (!workingDirectory.exists())
			throw new RuntimeException("Folder of Runner does not exist");
		String filename = "aicup2019.exe";
		File executable = new File(workingDirectory, filename);
		if (!executable.exists())
			throw new RuntimeException("aicup2109.exe does not exist");

		ProcessBuilder botProcess = new ProcessBuilder("java", "-cp", borJar.getAbsolutePath(), "Runner");
		botProcess.inheritIO();

		for (int i = 0; i < 10; i++) {
			File result = new File(workingDirectory, "save/a" + i);
			ProcessBuilder builder = new ProcessBuilder(executable.getAbsolutePath(), "--batch-mode", "--config",
					"config.json", "--save-results", "save/a" + i, "--save-replay", "replay/a" + i);
			builder.directory(workingDirectory);
			builder.inheritIO();
			builder.start();

			Process process = botProcess.start();

			process.waitFor();

			String resultContent = Files.lines(result.toPath()).collect(Collectors.joining(""));
			Result resultObj = gson.fromJson(resultContent, Result.class);
			System.out.println("crash " + resultObj.players[0].crashed + "/" + resultObj.players[0].crashed + " "
					+ resultObj.results[0] + "/" + resultObj.results[1]);
		}
	}
}
