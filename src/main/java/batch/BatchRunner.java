package batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class BatchRunner {
	public static void main(String[] args) throws IOException, InterruptedException {
		Gson gson = new Gson();
		File borJar = new File(args[1]);
		if (!borJar.exists())
			throw new RuntimeException("Bot jar does not exist");
		else
			System.out.println("Bot: " + borJar);
		File borJar2 = null;
		if (args.length == 3) {
			borJar2 = new File(args[2]);
			if (!borJar2.exists())
				throw new RuntimeException("Bot jar 2 does not exist");
			else
				System.out.println("Bot2: " + borJar2);
		}

		File workingDirectory = new File(args[0]);
		if (!workingDirectory.exists())
			throw new RuntimeException("Folder of Runner does not exist");
		else
			System.out.println("Working Directory: " + workingDirectory);

		new File(workingDirectory, "save").mkdir();
		new File(workingDirectory, "replay").mkdir();

		String filename = "aicup2019.exe";
		File executable = new File(workingDirectory, filename);
		if (!executable.exists())
			throw new RuntimeException("aicup2109.exe does not exist");

		ProcessBuilder botProcess = new ProcessBuilder("java", "-cp", borJar.getAbsolutePath(), "Runner");
		// botProcess.inheritIO();
		ProcessBuilder botProcess2 = null;
		if (borJar2 != null) {
			botProcess2 = new ProcessBuilder("java", "-cp", borJar2.getAbsolutePath(), "Runner", "127.0.0.1", "31000");
			// botProcess2.inheritIO();
			if (!new File(workingDirectory, "multiplayer.json").exists()) {
				throw new RuntimeException("missing multiplayer.json");
			}
		} else {
			if (!new File(workingDirectory, "config.json").exists()) {
				throw new RuntimeException("missing config.json");
			}
		}

		int[] win = new int[] { 0, 0 };
		int games = 500;
		for (int i = 0; i < games; i++) {
			File result = new File(workingDirectory, "save/a" + i);
			ProcessBuilder builder = new ProcessBuilder(executable.getAbsolutePath(), "--batch-mode", "--config", borJar2 != null ? "multiplayer.json" : "config.json", "--save-results", "save/a" + i, "--save-replay", "replay/a" + i);
			builder.directory(workingDirectory);
			builder.inheritIO();
			builder.start();
			System.out.println("started agent:" + builder.command().stream().collect(Collectors.joining(" ")));

			Process process = botProcess.start();
			System.out.println("started bot 1:" + botProcess.command().stream().collect(Collectors.joining(" ")));
			if (borJar2 != null) {
				process = botProcess2.start();
				System.out.println("started bot 2:" + botProcess2.command().stream().collect(Collectors.joining(" ")));
			}

			process.waitFor();

			String resultContent = Files.lines(result.toPath()).collect(Collectors.joining(""));
			Result resultObj = gson.fromJson(resultContent, Result.class);
			System.out.println("crash " + resultObj.players[0].crashed + "/" + resultObj.players[0].crashed + " " + resultObj.results[0] + "/" + resultObj.results[1]);

			if (resultObj.results[0] > resultObj.results[1]) {
				win[0]++;
			} else if (resultObj.results[0] < resultObj.results[1]) {
				win[1]++;
			}

		}
		System.out.println("Wins " + win[0] + "/" + win[1] + " " + (100 * win[0] / games) + "%/" + (100 * win[1] / games) + "%");
	}
}
