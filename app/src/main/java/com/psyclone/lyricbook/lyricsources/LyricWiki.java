package com.psyclone.lyricbook.lyricsources;

import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LyricWiki
{
    public static String encodeInUTF8(String original)
    {
        try
        {
            return URLEncoder.encode(original, "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return original;
        }
    }

    public static String fetchLyrics(String artist, String track)
    {
        try
        {
            String encodedArtist = encodeInUTF8(artist);
            String encodedTrack = encodeInUTF8(track);

            String baseURL = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";
            String urlToSearch = String.format(baseURL, encodedArtist, encodedTrack);

            URL apiUrl = new URL(urlToSearch);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = br.readLine();
                while(line != null)
                {
                    builder.append(line);
                    line = br.readLine();
                }
                br.close();
                String response = builder.toString();
                response = response.replace("song =", ""); //The JSON object returned by the API is preceded by 'song ='

                JSONObject json = new JSONObject(response);

                String finalUrl = json.getString("url"); //Actual URL for the LyricWiki page with the lyrics

                if(finalUrl.contains("action=edit")) //The API gives a link to edit the lyrics which it couldn't find
                    return "ERROR: Lyrics not available on LyricWiki";

                Document lyricsPage = Jsoup.connect(finalUrl).get();

                Element lyricbox = lyricsPage.select("div.lyricbox").get(0); //No need of matching pattern like AZLyrics because here the lyrics div has a class
                lyricbox.getElementsByClass("references").remove(); //Unnecessary data which the user doesn't need to see
                String lyricsHtml = lyricbox.html();

                //Different method because LyricWiki stores every character in the lyrics as HTML entities
                Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
                String lyrics = Jsoup.clean(lyricsHtml, "", new Whitelist().addTags("br"), outputSettings);
                lyrics = Parser.unescapeEntities(lyrics, true);
                lyrics = Html.fromHtml(lyrics).toString().trim();

                if (lyrics.equals(""))
                    return "ERROR: Blank lyrics";

                return lyrics;
            }
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
        }
        return "ERROR: Couldn't fetch lyrics";
    }
}