package com.gmail.jonathan.klamroth.srit;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by jonny on 24.06.16.
 */
public class SmaliResIdTool {

    private final Pattern RES_ID_COMMENT_PATTERN = Pattern.compile(".*# resource/[a-z]+: .*");
    private final Pattern CONST_PATTERN = Pattern.compile(".*const +v[0-9]+, *0x([a-fA-F0-9]+).*");

    private final File mProjectDirFile;
    private final String mLang;

    private final String mValuesDir;


    public SmaliResIdTool (File projectDirFile, String lang) {
        mProjectDirFile = projectDirFile;
        mLang = lang;

        mValuesDir = "res/values" + (lang.length() > 0 ? "-" + lang : "") + "/";
    }


    public void run () {
        System.out.println("> read resource ids");
        HashMap<Integer, Resource> resources = readResources();
        if (resources == null)
            Util.exitWithError("cannot parse resources");


        System.out.println("> read strings");
        HashMap<String, String> strings = readStrings();
        if (strings == null)
            Util.exitWithError("cannot parse strings");

        System.out.println("> read integers");
        HashMap<String, String> integers = readIntegers();
        if (integers == null)
            Util.exitWithError("cannot parse integer");

        System.out.println("> read bools");
        HashMap<String, String> bools = readBools();
        if (bools == null)
            Util.exitWithError("cannot parse bools");

        System.out.println("> read colors");
        HashMap<String, String> colors = readColors();
        if (colors == null)
            Util.exitWithError("cannot parse colors");

        System.out.println("> read dimens");
        HashMap<String, String> dimens = readDimens();
        if (dimens == null)
            Util.exitWithError("cannot parse dimens");


        System.out.println("> process folder smali");
        File smaliDirFile = new File(mProjectDirFile, "smali");
        try {
            processFolder(smaliDirFile, resources, strings, integers, bools, colors, dimens);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i=2; ; i++) {
            smaliDirFile = new File(mProjectDirFile, "smali_classes" + i);
            if (!smaliDirFile.isDirectory())
                break;

            System.out.println("> process folder smali_classes" + i);
            try {
                processFolder(smaliDirFile, resources, strings, integers, bools, colors, dimens);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("> done");
    }


    private void processFolder (File dir, HashMap<Integer, Resource> resources, HashMap<String, String> strings, HashMap<String, String> integers, HashMap<String, String> bools, HashMap<String, String> colors, HashMap<String, String> dimens) throws IOException {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            if (f.isDirectory())
                processFolder(f, resources, strings, integers, bools, colors, dimens);
            else
                processFile(f, resources, strings, integers, bools, colors, dimens);
        }
    }

    private void processFile (File f, HashMap<Integer, Resource> resources, HashMap<String, String> strings, HashMap<String, String> integers, HashMap<String, String> bools, HashMap<String, String> colors, HashMap<String, String> dimens) throws IOException {
        StringBuilder sb = new StringBuilder();

        new FileLineReader(f){
            private boolean mIgnoreLines = false;

            @Override
            public void lineRead (String line) {
                Matcher m = RES_ID_COMMENT_PATTERN.matcher(line);
                if (m.matches()) {
                    mIgnoreLines = true;
                    return;
                }

                m = CONST_PATTERN.matcher(line);
                if (m.matches()) {
                    int id = Integer.parseInt(m.group(1), 16);
                    if (resources.containsKey(id)) {
                        Resource res = resources.get(id);
                        sb.append("    # resource/").append(res.type).append(": ").append(res.name).append("\n");

                        if (res.type.equals("string") && strings.containsKey(res.name))
                            sb.append("    # ").append(strings.get(res.name).replace("\r", "\\r").replace("\n", "\\n")).append("\n");
                        else if (res.type.equals("integer") && integers.containsKey(res.name))
                            sb.append("    # ").append(integers.get(res.name).replace("\r", "\\r").replace("\n", "\\n")).append("\n");
                        else if (res.type.equals("bool") && bools.containsKey(res.name))
                            sb.append("    # ").append(bools.get(res.name).replace("\r", "\\r").replace("\n", "\\n")).append("\n");
                        else if (res.type.equals("color") && colors.containsKey(res.name))
                            sb.append("    # ").append(colors.get(res.name).replace("\r", "\\r").replace("\n", "\\n")).append("\n");
                        else if (res.type.equals("dimen") && dimens.containsKey(res.name))
                            sb.append("    # ").append(dimens.get(res.name).replace("\r", "\\r").replace("\n", "\\n")).append("\n");
                    }

                    mIgnoreLines = false;
                }

                if (!mIgnoreLines)
                    sb.append(line).append("\n");
            }
        };

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(sb.toString());
        }
    }


    private void parseResource (File file, ContentHandler contentHandler) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(contentHandler);
        xmlReader.parse(new InputSource(new FileReader(file)));
    }


    private HashMap<String, String> readStrings () {
        File stringsFile = checkFileReadable(mValuesDir + "strings.xml");
        File defaultStringsFile = (mLang.length() > 0 ? checkFileReadable("res/values/strings.xml") : null);
        HashMap<String, String> strings = new HashMap<>();

        try {
            if (stringsFile != null)
                parseResource(stringsFile, new StringResourceHandler("string", strings));

            if (defaultStringsFile != null)
                parseResource(defaultStringsFile, new StringResourceHandler("string", strings));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return strings;
    }

    private HashMap<String, String> readIntegers () {
        File integersFile = checkFileReadable(mValuesDir + "integers.xml");
        File defaultIntegersFile = (mLang.length() > 0 ? checkFileReadable("res/values/integers.xml") : null);
        HashMap<String, String> integers = new HashMap<>();

        try {
            if (integersFile != null)
                parseResource(integersFile, new StringResourceHandler("integer", integers));

            if (defaultIntegersFile != null)
                parseResource(defaultIntegersFile, new StringResourceHandler("integer", integers));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return integers;
    }

    private HashMap<String, String> readBools () {
        File boolsFile = checkFileReadable(mValuesDir + "bools.xml");
        File defaultBoolsFile = (mLang.length() > 0 ? checkFileReadable("res/values/bools.xml") : null);
        HashMap<String, String> bools = new HashMap<>();

        try {
            if (boolsFile != null)
                parseResource(boolsFile, new StringResourceHandler("bool", bools));

            if (defaultBoolsFile != null)
                parseResource(defaultBoolsFile, new StringResourceHandler("bool", bools));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return bools;
    }

    private HashMap<String, String> readColors () {
        File colorsFile = checkFileReadable(mValuesDir + "colors.xml");
        File defaultColorsFile = (mLang.length() > 0 ? checkFileReadable("res/values/colors.xml") : null);
        HashMap<String, String> colors = new HashMap<>();

        try {
            if (colorsFile != null)
                parseResource(colorsFile, new StringResourceHandler("color", colors));

            if (defaultColorsFile != null)
                parseResource(defaultColorsFile, new StringResourceHandler("color", colors));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return colors;
    }

    private HashMap<String, String> readDimens () {
        File dimensFile = checkFileReadable(mValuesDir + "dimens.xml");
        File defaultDimensFile = (mLang.length() > 0 ? checkFileReadable("res/values/dimens.xml") : null);
        HashMap<String, String> dimens = new HashMap<>();

        try {
            if (dimensFile != null)
                parseResource(dimensFile, new StringResourceHandler("dimen", dimens));

            if (defaultDimensFile != null)
                parseResource(defaultDimensFile, new StringResourceHandler("dimen", dimens));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return dimens;
    }


    private HashMap<Integer, Resource> readResources () {
        return readPublicXml();
    }

    private HashMap<Integer, Resource> readPublicXml () {
        File publicXmlFile = checkFileReadable("res/values/public.xml", true);
        HashMap<Integer, Resource> resources = new HashMap<>();

        try {
            parseResource(publicXmlFile, new ResourceIdHandler(resources));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return resources;
    }


    private File checkFileReadable (String name) {
        return checkFileReadable(name, false);
    }

    private File checkFileReadable (String name, boolean requireReadAccess) {
        File f = new File(mProjectDirFile, name);
        if (!f.isFile() || !f.canRead()) {
            if (requireReadAccess)
                Util.exitWithError(name + " not found or readable");
            else
                return null;
        }

        return f;
    }


    private static class StringResourceHandler extends DefaultHandler {

        private final String mType;
        private final HashMap<String, String> mData;

        private String mName = null;
        private String mValue = null;


        public StringResourceHandler (String type, HashMap<String, String> data) {
            mType = type;
            mData = data;
        }


        @Override
        public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals(mType)) {
                mName = attributes.getValue("name");
                mValue = "";
            }
        }

        @Override
        public void endElement (String uri, String localName, String qName) throws SAXException {
            if (qName.equals(mType) && mName != null) {
                if (!mData.containsKey(mName))
                    mData.put(mName, mValue);

                mName = null;
            }
        }

        @Override
        public void characters (char[] ch, int start, int length) throws SAXException {
            if (mName != null)
                mValue += new String(ch, start, length);
        }

    }

    private static class ResourceIdHandler extends DefaultHandler {

        private final HashMap<Integer, Resource> mResources;


        public ResourceIdHandler (HashMap<Integer, Resource> resources) {
            mResources = resources;
        }


        @Override
        public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("public")) {
                String type = attributes.getValue("type");
                String name = attributes.getValue("name");
                String idStr = attributes.getValue("id");

                if (type == null || name == null || idStr == null) {
                    System.out.println("warning: incomplete public entry");
                    return;
                }

                int id;
                try {
                    id = Integer.parseInt(idStr.substring(2), 16);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    System.out.println("warning: invalid id " + idStr);
                    return;
                }


                if (mResources.containsKey(id)) {
                    System.out.println("warning: duplicate id 0x" + Integer.toHexString(id));
                    return;
                }

                mResources.put(id, new Resource(name, type));
            }
        }

    }



    private static class Resource {

        public final String name;
        public final String type;


        public Resource (String name, String type) {
            this.name = name;
            this.type = type;
        }

    }

}
