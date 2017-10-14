package com.psyclone.lyricbook.lyricsources;

import android.text.Html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AZLyrics
{
    private static String getArtist(String artist)
    {
        if(artist.toLowerCase().startsWith("the "))
            artist = artist.substring(4, artist.length());

        return artist
                .toLowerCase()
                .replaceAll("&", "and")
                .replaceAll("[^a-z0-9]+", "") //remove any character which is not between a-z or 0-9
                .split("feat")[0]; //remove the featured artist(s)
    }

    private static String getTrack(String track)
    {
        return track
                .toLowerCase()
                .replaceAll("&", "and")
                .replaceAll("[^a-z0-9]+", "") //remove any character which is not between a-z or 0-9
                .split("feat")[0]; //remove the featured artist(s) from the title
    }

    public static String fetchLyrics(String artist, String track)
    {
        String urlArtist = getArtist(artist);
        String urlTrack = getTrack(track);
        String urlToSearch = String.format("http://www.azlyrics.com/lyrics/%s/%s.html", urlArtist, urlTrack);
        String html;
        try
        {
            Document document = Jsoup.connect(urlToSearch).userAgent("").timeout(3000).get();
            html = document.html();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "ERROR: IOException";
        }

        Pattern pattern = Pattern.compile("Sorry about that. -->(.*)", Pattern.DOTALL); //This is the last html tag before the actual lyrics begin
        Matcher matcher = pattern.matcher(html);

        if (matcher.find())
        {
            String lyrics = matcher.group(1);
            lyrics = lyrics.substring(0, lyrics.indexOf("</div>"))
                    .replaceAll("\\[[^\\[]*\\]", "");  //remove special characters from the lyrics
            lyrics = Html.fromHtml(lyrics).toString().trim().replaceAll("\n\n\n", "\n\n"); //removes HTML tags like <br />, <b>, etc.
            return lyrics;
        }
        else
            return "ERROR: Matcher couldn't find the lyrics";
    }
}
