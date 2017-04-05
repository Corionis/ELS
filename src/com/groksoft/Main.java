package com.groksoft;
// http://javarevisited.blogspot.com/2014/12/how-to-read-write-json-string-to-file.html

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

// https://github.com/cliftonlabs/json-simple
import org.json.simple.*;

public class Main
{

    public static void main(String[] args) {
        String y;
        String file = "";

        try {
            System.out.println("Reading JSON file");
            FileReader fileReader = new FileReader(file);
            Object o = Jsoner.deserialize(fileReader);
            //JSONObject json = (JSONObject) parser.parse(fileReader);



        } catch (Exception e)
        {

        }
    }
