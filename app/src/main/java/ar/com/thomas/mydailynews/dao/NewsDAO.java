package ar.com.thomas.mydailynews.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.sql.RowId;
import java.util.ArrayList;
import java.util.List;

import ar.com.thomas.mydailynews.model.News;
import ar.com.thomas.mydailynews.util.DAOException;
import ar.com.thomas.mydailynews.util.HTTPConnectionManager;
import ar.com.thomas.mydailynews.util.ResultListener;

/**
 * Created by alejandrothomas on 6/27/16.
 */
public class NewsDAO extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "NewsDB";
    private static final Integer DATABASE_VERSION = 1;

    private static final String TABLE_NEWS = "News";
    private static final String ID = "ID";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE_URL = "imageURL";
    private static final String RSS_FEED = "rssFeed";

    private Context context;

    public NewsDAO(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);

    }


    //------------------OFFLINE--------------------//
    @Override
    public void onCreate(SQLiteDatabase db) {

        //Creo la tabla que contendrá mi base de datos
        String createTable = "CREATE TABLE " + TABLE_NEWS + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TITLE + " TEXT, "
                + IMAGE_URL + " TEXT, "
                + DESCRIPTION + " TEXT " + ")";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addNewsListToDB(List<News> newsList) {
        for (News news : newsList) {
            if(!checkIfExist(news.getNewsID())) {
                this.addNewsToDB(news);
            }
        }
    }

    private Boolean checkIfExist(Integer newsID){

        SQLiteDatabase database = getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_NEWS
                + " WHERE " + ID + "==" + newsID;

        Cursor result = database.rawQuery(selectQuery, null);
        Integer count = result.getCount();

        Log.v("NewsDAO", "La noticia " + newsID + " ya esta en la base");

        database.close();

        return (count > 0);
    }

    public void addNewsToDB(News news){

        SQLiteDatabase database = getWritableDatabase();

        ContentValues row = new ContentValues();



        row.put(TITLE, news.getTitle());
        row.put(DESCRIPTION, news.getDescription());
        row.put(IMAGE_URL, news.getImageUrl());

        database.insert(TABLE_NEWS, null, row);

        database.close();
    }



    public List<News> getNewsListFromDatabase(String rssFeed){

        SQLiteDatabase database = getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_NEWS;
//                + " WHERE " + RSS_FEED + "==" + rssFeed;
        Cursor cursor = database.rawQuery(selectQuery, null);

        List<News> newsList = new ArrayList<>();

        while(cursor.moveToNext()){

            News news = new News();

            news.setNewsID(cursor.getInt(cursor.getColumnIndex(ID)));
            news.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            news.setImageUrl(cursor.getString(cursor.getColumnIndex(IMAGE_URL)));
            news.setDescription(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));

            newsList.add(news);
        }

        return newsList;
    }

    //---------------------ONLINE------------------//

    public void getNewsList(ResultListener<List<News>> listener, String feedLink) {

        RetrieveFeedTask retrieveFeedTask = new RetrieveFeedTask(listener, feedLink);
        retrieveFeedTask.execute();
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, List<News>> {

        private ResultListener<List<News>> listener;

        private String feedLink;

        public RetrieveFeedTask(ResultListener<List<News>> listener, String feedLink) {
            this.listener = listener;
            this.feedLink = feedLink;
        }



        @Override
        protected List<News> doInBackground(String... params) {

            HTTPConnectionManager connectionManager = new HTTPConnectionManager();
            InputStream input = null;

            try {
                input = connectionManager.getRequestStream(feedLink);
            } catch (DAOException e) {
                e.printStackTrace();
            }
            List<News> result = new ArrayList<>();
            News aNews = null;
            XmlPullParser parser = Xml.newPullParser();
            try {
                parser.setInput(input, null);
                Integer status = parser.getEventType();
                while (status != XmlPullParser.END_DOCUMENT) {
                    switch (status) {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            Log.d("TAG", parser.getName());
                            if (parser.getName().equals("item")) {
                                aNews = new News();
                            }
                            if (aNews != null) {
                                if (parser.getName().equals("title")) {
                                    aNews.setTitle(parser.nextText());
                                } else if (parser.getName().equals("link")) {
                                    aNews.setLink(parser.nextText());
                                } else if (parser.getName().equals("description")) {
                                    Log.d("TAG", parser.getName());
                                    aNews.setDescription(parser.nextText());
                                } else if (parser.getName().equals("pubDate")) {
                                    Log.d("TAG", parser.getName());
                                    aNews.setPubDate(parser.nextText());
                                } else if (parser.getName().equals("author")) {
                                    Log.d("TAG", parser.getName());
                                    aNews.setAuthor(parser.nextText());
                                } else if (parser.getName().equals("enclosure")) {
                                    Log.d("TAG", parser.getName());
                                    aNews.setImageUrl(parser.getAttributeValue(null, "url"));
                                }
                            }
                            Log.v("INFO", parser.getName());
                            break;
                        case XmlPullParser.END_TAG:
                            Log.d("TAG", parser.getName());
                            if (parser.getName().equals("item")) {
                                result.add(aNews);
                                aNews = null;
                            }
                            break;
                    }
                    status = parser.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<News> input) {

            addNewsListToDB(input);
            this.listener.finish(input);
        }
    }
}

