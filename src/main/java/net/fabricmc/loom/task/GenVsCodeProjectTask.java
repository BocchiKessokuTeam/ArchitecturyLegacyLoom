/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2021 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ide.RunConfig;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;

// Recommended vscode plugins:
// https://marketplace.visualstudio.com/items?itemName=redhat.java
// https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug
// https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack
public class GenVsCodeProjectTask extends AbstractLoomTask {
	@TaskAction
	public void genRuns() {
		clean(getProject());
		generate(getProject());
	}

	public static void clean(Project project) {
		File projectDir = project.getRootProject().file(".vscode");

		if (!projectDir.exists()) {
			projectDir.mkdir();
		}

		File launchJson = new File(projectDir, "launch.json");

		if (launchJson.exists()) {
			launchJson.delete();
		}
	}

	public static void generate(Project project) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		File projectDir = project.getRootProject().file(".vscode");

		if (!projectDir.exists()) {
			projectDir.mkdir();
		}

		File launchJson = new File(projectDir, "launch.json");
		File tasksJson = new File(projectDir, "tasks.json");

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		VsCodeLaunch launch;

		if (launchJson.exists()) {
			try {
				launch = gson.fromJson(FileUtils.readFileToString(launchJson, StandardCharsets.UTF_8), VsCodeLaunch.class);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read launch.json", e);
			}
		} else {
			launch = new VsCodeLaunch();
		}

		for (RunConfigSettings settings : extension.getRunConfigs()) {
			if (!settings.isIdeConfigGenerated()) {
				continue;
			}

			launch.add(project, RunConfig.runConfig(project, settings));
			settings.makeRunDir();
		}

		String json = LoomGradlePlugin.GSON.toJson(launch);

		try {
			FileUtils.writeStringToFile(launchJson, json, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write launch.json", e);
		}

		VsCodeTasks tasks;

		if (tasksJson.exists()) {
			try {
				tasks = gson.fromJson(FileUtils.readFileToString(tasksJson, StandardCharsets.UTF_8), VsCodeTasks.class);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read launch.json", e);
			}
		} else {
			tasks = new VsCodeTasks();
		}

		for (VsCodeConfiguration configuration : launch.configurations) {
			if (configuration.preLaunchTask != null && configuration.tasksBeforeRun != null) {
				String prefix = Os.isFamily(Os.FAMILY_WINDOWS) ? "gradlew.bat" : "./gradlew";
				tasks.add(new VsCodeTask(configuration.preLaunchTask, prefix + " " + configuration.tasksBeforeRun.stream()
						.map(s -> {
							int i = s.indexOf('/');
							return i == -1 ? s : s.substring(i + 1);
						}).collect(Collectors.joining(" ")), "shell", new String[0]));
			}
		}

		if (!tasks.tasks.isEmpty()) {
			String jsonTasks = gson.toJson(tasks);

			try {
				FileUtils.writeStringToFile(tasksJson, jsonTasks, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new RuntimeException("Failed to write tasks.json", e);
			}
		}
	}

	private static class VsCodeLaunch {
		public String version = "0.2.0";
		public List<VsCodeConfiguration> configurations = new ArrayList<>();

		public void add(Project project, RunConfig runConfig) {
			if (configurations.stream().noneMatch(config -> Objects.equals(config.name, runConfig.configName))) {
				VsCodeConfiguration configuration = new VsCodeConfiguration(project, runConfig);
				configurations.add(configuration);

				if (!configuration.tasksBeforeRun.isEmpty()) {
					configuration.preLaunchTask = "generated_" + runConfig.configName;
				}
			}
		}
	}

	private static class VsCodeTasks {
		public String version = "2.0.0";
		public List<VsCodeTask> tasks = new ArrayList<>();

		public void add(VsCodeTask vsCodeTask) {
			if (tasks.stream().noneMatch(task -> Objects.equals(task.label, vsCodeTask.label))) {
				tasks.add(vsCodeTask);
			}
		}
	}

	@SuppressWarnings("unused")
	private static class VsCodeConfiguration {
		public transient Project project;
		public String type = "java";
		public String name;
		public String request = "launch";
		public String cwd;
		public String console = "internalConsole";
		public boolean stopOnEntry = false;
		public String mainClass;
		public String vmArgs;
		public String args;
		public Map<String, String> env = new LinkedHashMap<>();
		public transient List<String> tasksBeforeRun = new ArrayList<>();
		public String preLaunchTask = null;
		public String projectName = null;

		VsCodeConfiguration(Project project, RunConfig runConfig) {
			this.name = runConfig.configName;
			this.mainClass = runConfig.mainClass;
			this.vmArgs = RunConfig.joinArguments(runConfig.vmArgs);
			this.args = RunConfig.joinArguments(runConfig.programArgs);
			this.cwd = "${workspaceFolder}/" + runConfig.runDir;
			this.projectName = runConfig.vscodeProjectName;
			this.env.putAll(runConfig.envVariables);
			this.tasksBeforeRun.addAll(runConfig.vscodeBeforeRun);

			if (project.getRootProject() != project) {
				Path rootPath = project.getRootDir().toPath();
				Path projectPath = project.getProjectDir().toPath();
				String relativePath = rootPath.relativize(projectPath).toString();

				this.cwd = "${workspaceFolder}/%s/%s".formatted(relativePath, runConfig.runDir);
			}
		}
	}

	private static class VsCodeTask {
		public String label;
		public String command;
		public String type;
		public String[] args;
		public String group = "build";

		VsCodeTask(String label, String command, String type, String[] args) {
			this.label = label;
			this.command = command;
			this.type = type;
			this.args = args;
		}
	}
}
