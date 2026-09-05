package com.yision.fluidlogistics.infrastructure.data;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.loading.FMLPaths;

public class FluidLogisticsDatagen {
	private static final Path DATA_TEMPLATES = FMLPaths.GAMEDIR.get().getParent()
		.resolve("src/datagen/resources/data");

	public static void gatherData(GatherDataEvent event) {
		event.getGenerator().addProvider(event.includeServer(),
			new StaticDataProvider(event.getGenerator().getPackOutput()));
	}

	private static class StaticDataProvider implements DataProvider {
		private final PackOutput output;

		private StaticDataProvider(PackOutput output) {
			this.output = output;
		}

		@Override
		public CompletableFuture<?> run(CachedOutput cache) {
			try (Stream<Path> paths = Files.walk(DATA_TEMPLATES)) {
				return CompletableFuture.allOf(paths
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".json"))
					.map(path -> (CompletableFuture<?>) save(cache, path))
					.toArray(CompletableFuture[]::new));
			} catch (IOException exception) {
				throw new UncheckedIOException("Failed to read data templates from " + DATA_TEMPLATES, exception);
			}
		}

		private CompletableFuture<?> save(CachedOutput cache, Path template) {
			try (Reader reader = Files.newBufferedReader(template)) {
				JsonElement json = JsonParser.parseReader(reader);
				Path relative = DATA_TEMPLATES.relativize(template);
				Path destination = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(relative);
				return DataProvider.saveStable(cache, json, destination);
			} catch (IOException exception) {
				throw new UncheckedIOException("Failed to read data template " + template, exception);
			}
		}

		@Override
		public String getName() {
			return "FluidLogistics static data";
		}
	}
}
