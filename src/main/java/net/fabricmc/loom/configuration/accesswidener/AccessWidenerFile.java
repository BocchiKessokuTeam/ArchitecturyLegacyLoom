/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.loom.configuration.accesswidener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.loom.util.ZipUtils;

public record AccessWidenerFile(
		String path,
		String modId,
		byte[] content
) {
	/**
	 * Reads the access-widener contained in a mod jar, or returns null if there is none.
	 */
	public static AccessWidenerFile fromModJar(Path modJarPath) {
		byte[] modJsonBytes;

		try {
			modJsonBytes = ZipUtils.unpackNullable(modJarPath, "fabric.mod.json");
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read access-widener file from: " + modJarPath.toAbsolutePath(), e);
		}

		if (modJsonBytes == null) {
			if (ZipUtils.contains(modJarPath, "architectury.common.json")) {
				String awPath = null;
				byte[] commonJsonBytes;

				try {
					commonJsonBytes = ZipUtils.unpackNullable(modJarPath, "architectury.common.json");
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to read architectury.common.json file from: " + modJarPath.toAbsolutePath(), e);
				}

				if (commonJsonBytes != null) {
					JsonObject jsonObject = new Gson().fromJson(new String(commonJsonBytes, StandardCharsets.UTF_8), JsonObject.class);

					if (jsonObject.has("accessWidener")) {
						awPath = jsonObject.get("accessWidener").getAsString();
					} else {
						return null;
					}
				} else {
					// ???????????
					throw new IllegalArgumentException("The architectury.common.json file does not exist.");
				}

				byte[] content;

				try {
					content = ZipUtils.unpack(modJarPath, awPath);
				} catch (IOException e) {
					throw new UncheckedIOException("Could not find access widener file (%s) defined in the architectury.common.json file of %s".formatted(awPath, modJarPath.toAbsolutePath()), e);
				}

				return new AccessWidenerFile(
						awPath,
						modJarPath.getFileName().toString(),
						content
				);
			}

			if (ZipUtils.contains(modJarPath, "quilt.mod.json")) {
				String awPath = null;
				byte[] quiltModBytes;

				try {
					quiltModBytes = ZipUtils.unpackNullable(modJarPath, "quilt.mod.json");
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to read quilt.mod.json file from: " + modJarPath.toAbsolutePath(), e);
				}

				if (quiltModBytes != null) {
					JsonObject jsonObject = new Gson().fromJson(new String(quiltModBytes, StandardCharsets.UTF_8), JsonObject.class);

					if (jsonObject.has("access_widener")) {
						if (jsonObject.get("access_widener").isJsonArray()) {
							JsonArray array = jsonObject.get("access_widener").getAsJsonArray();

							if (array.size() != 1) {
								throw new UnsupportedOperationException("Loom does not support multiple access wideners in one mod!");
							}

							awPath = array.get(0).getAsString();
						} else {
							awPath = jsonObject.get("access_widener").getAsString();
						}
					} else {
						return null;
					}
				} else {
					// ???????????
					throw new IllegalArgumentException("The quilt.mod.json file does not exist.");
				}

				byte[] content;

				try {
					content = ZipUtils.unpack(modJarPath, awPath);
				} catch (IOException e) {
					throw new UncheckedIOException("Could not find access widener file (%s) defined in the quilt.mod.json file of %s".formatted(awPath, modJarPath.toAbsolutePath()), e);
				}

				return new AccessWidenerFile(
						awPath,
						modJarPath.getFileName().toString(),
						content
				);
			}

			return null;
		}

		JsonObject jsonObject = new Gson().fromJson(new String(modJsonBytes, StandardCharsets.UTF_8), JsonObject.class);

		if (!jsonObject.has("accessWidener")) {
			return null;
		}

		String awPath = jsonObject.get("accessWidener").getAsString();
		String modId = jsonObject.get("id").getAsString();

		byte[] content;

		try {
			content = ZipUtils.unpack(modJarPath, awPath);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not find access widener file (%s) defined in the fabric.mod.json file of %s".formatted(awPath, modJarPath.toAbsolutePath()), e);
		}

		return new AccessWidenerFile(
				awPath,
				modId,
				content
		);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(path, modId);
		result = 31 * result + Arrays.hashCode(content);
		return result;
	}
}
