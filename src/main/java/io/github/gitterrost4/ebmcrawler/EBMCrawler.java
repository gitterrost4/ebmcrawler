package io.github.gitterrost4.ebmcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EBMCrawler {

    private record EBMItem(String number, String name, String abrBest, Map<String,List<String>> behandlungsAusschluss){
        public String toCSVLine(Set<String> behandlungsAusschlussHeaders){
            return number + CSV_SEPARATOR +
                    name + CSV_SEPARATOR +
                    abrBest + CSV_SEPARATOR +
                    behandlungsAusschlussHeaders.stream().sorted().map(h -> String.join(",", behandlungsAusschluss.getOrDefault(h, new ArrayList<>()))).collect(Collectors.joining(CSV_SEPARATOR));
        }
    }

    private static final String CSV_SEPARATOR = ";";

    public static void main(String[] args) throws IOException {
        String ebmDir = args.length==0 ? "." : args[0];
        String outFilePath = args.length < 2 ? "/tmp/ebm.csv" : args [1];

        File[] files = new File(ebmDir).listFiles((dir, name) -> name.chars().limit(5).allMatch(Character::isDigit));
        if(files == null){
            throw new IOException("Could not read from path "+ebmDir);
        }
        List<EBMItem> ebmItems = Arrays.stream(files).map(EBMCrawler::parseHtmlFile).flatMap(Optional::stream).toList();
        Set<String> behAusschlussHeaders = ebmItems.stream().flatMap(e->e.behandlungsAusschluss().keySet().stream()).collect(Collectors.toSet());

        String headerLine = "number"+CSV_SEPARATOR+"title"+CSV_SEPARATOR+"Abrechnungsbestimmung"+CSV_SEPARATOR+ String.join(CSV_SEPARATOR, behAusschlussHeaders);
        String csvString = Stream.concat(Stream.of(headerLine), ebmItems.stream().map(e -> e.toCSVLine(behAusschlussHeaders))).collect(Collectors.joining("\n"));
        Files.writeString(Paths.get(outFilePath), csvString);
    }

    public static Optional<EBMItem> parseHtmlFile(File inputFile) {
        try {
            Document doc = Jsoup.parse(inputFile, StandardCharsets.UTF_8.toString());
            Elements headElements = doc.select("td.ebm_head");
            if (headElements.size() != 2) {
                throw new IllegalStateException("not exactly two head columns");
            }
            String number = headElements.stream().map(Element::html).filter(x -> x.chars().allMatch(Character::isDigit)).findFirst().orElseThrow();
            String title = headElements.stream().map(Element::html).filter(x -> !x.chars().allMatch(Character::isDigit)).findFirst().orElseThrow();

            Optional<Element> abrBestimmungHeader = doc.select("p.ebm_section").stream().filter(e -> e.hasText() && e.html().equals("Abrechnungsbestimmung")).findFirst();

            String abrBest = abrBestimmungHeader.map(Element::nextElementSibling).filter(Element::hasText).map(Element::html).orElse("");

            Elements behAusschlussKeys = doc.select("td.ebm_abrbestleft");

            Map<String, List<String>> behAusschluss = behAusschlussKeys.stream().collect(Collectors.toMap(Element::html, k -> k.nextElementSibling().select("a").stream().map(Element::html).collect(Collectors.toList())));

            return Optional.of(new EBMItem(number, title, abrBest, behAusschluss));
        } catch (Exception e){
            System.err.println("Error in file " + inputFile.getName()+". Ignoring...");
            return Optional.empty();
        }
    }
}