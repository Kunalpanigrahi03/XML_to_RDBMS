package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

public class xmlValidator {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java xmlValidator <filename>");
            return;
        }

        String filename = args[0];
        try {
            validateXML(filename);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void validateXML(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        Stack<TagInfo> stack = new Stack<>();
        boolean hasRoot = false;
        int lineNumber = 0;

        while ((line = br.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("<?xml")) continue;

            int i = 0;
            while (i < line.length()) {
                if (line.charAt(i) == '<') {
                    int end = line.indexOf('>', i);
                    if (i + 1 < line.length() && line.charAt(i + 1) == '/') {
                        if (end == -1) {
                            reportError(lineNumber, "Missing closing '>' for closing tag");
                            return;
                        }
                        String closingTag = line.substring(i + 2, end).trim();
                        if (stack.isEmpty() || !stack.peek().name.equals(closingTag)) {
                            reportError(lineNumber, "Unmatched closing tag: " + closingTag);
                            return;
                        }
                        stack.pop();
                    } else {
                        if (end == -1) {
                            reportError(lineNumber, "Missing closing '>' for opening tag");
                            return;
                        }
                        String openingTag = line.substring(i + 1, end).trim();
                        int spaceIndex = openingTag.indexOf(' ');
                        if (spaceIndex != -1) {
                            openingTag = openingTag.substring(0, spaceIndex);
                        }

                        boolean isSelfClosing = openingTag.endsWith("/") || line.charAt(end - 1) == '/';
                        if (openingTag.endsWith("/")) {
                            openingTag = openingTag.substring(0, openingTag.length() - 1);
                        }

                        if (!isSelfClosing) {
                            stack.push(new TagInfo(openingTag, lineNumber));
                        }

                        if (!hasRoot) {
                            hasRoot = true;
                        }
                    }
                    i = end + 1;
                } else {
                    i++;
                }
            }
        }
        br.close();

        if (!stack.isEmpty()) {
            TagInfo unclosedTag = stack.peek();
            reportError(unclosedTag.lineNumber, "Unclosed tag: " + unclosedTag.name);
            return;
        }

        if (!hasRoot) {
            reportError(lineNumber, "XML document has no root element");
            return;
        }

        System.out.println("The XML file is well-formed");
    }

    private static void reportError(int lineNumber, String message) {
        System.err.println("Error on line " + lineNumber + ": " + message);
    }

    private static class TagInfo {
        String name;
        int lineNumber;

        TagInfo(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
}