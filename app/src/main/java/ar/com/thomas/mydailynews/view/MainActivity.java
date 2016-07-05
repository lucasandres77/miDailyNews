package ar.com.thomas.mydailynews.view;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import ar.com.thomas.mydailynews.R;
import ar.com.thomas.mydailynews.controller.NewsController;
import ar.com.thomas.mydailynews.dao.RSSFeedCategoryDAO;
import ar.com.thomas.mydailynews.model.RSSFeed;
import ar.com.thomas.mydailynews.model.RSSFeedCategory;
import ar.com.thomas.mydailynews.view.FavouriteFlow.FragmentFavouriteContainer;
import ar.com.thomas.mydailynews.view.NewsFlow.FragmentNewsContainer;
import ar.com.thomas.mydailynews.view.RSSFeedFlow.FragmentRSSFeedContainer;
import ar.com.thomas.mydailynews.view.RSSFeedFlow.FragmentRSSFeedViewPager;

public class MainActivity extends AppCompatActivity implements FragmentRSSFeedViewPager.FragmentCalls, FragmentRSSFeedContainer.FavouriteCalls{

    protected Context context;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private DrawerLayout drawerLayout;
    private List<RSSFeedCategory> rssFeedCategoryList;
    private NavigationView navigationView;
    private List<String> favouriteListMainActivity;
    private FragmentRSSFeedContainer fragmentRSSFeedContainer;
    private FragmentFavouriteContainer fragmentFavouriteContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        Toast.makeText(context,getString(R.string.welcome),Toast.LENGTH_LONG).show();

        favouriteListMainActivity = new ArrayList<>();
        NewsController newsController = new NewsController();
        newsController.clearNewsDB(context);
        List<RSSFeed> rssFeedList = newsController.getFavouritesFromDB(context);
        for (RSSFeed rssFeed:rssFeedList){
            favouriteListMainActivity.add(rssFeed.getTitle());
        }
        newsController.updateFavourites(favouriteListMainActivity,context);


        Window window = getWindow();
        window.setStatusBarColor(0xFF37474F);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();



        navigationView=(NavigationView)findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new ListenerMenu());

        populateNavigationDrawerMenu();

        ListenerMenu listenerMenu = new ListenerMenu();
        listenerMenu.onNavigationItemSelected(navigationView.getMenu().getItem(15));
        navigationView.getMenu().getItem(15).setChecked(true);
    }

    @Override
    public void getNotifications(String newsClickedID, Integer itemPosition, String rssFeedID) {

        FragmentNewsContainer fragmentNewsContainer = new FragmentNewsContainer();
        Bundle arguments = new Bundle();
        arguments.putString(FragmentNewsContainer.NEWS_TITLE_ID, newsClickedID);
        arguments.putInt(FragmentNewsContainer.POSITION, itemPosition);
        arguments.putString(FragmentNewsContainer.RSS_SOURCE, rssFeedID);

        fragmentNewsContainer.setArguments(arguments);

        fragmentTransaction = fragmentManager.beginTransaction();

        if(getSupportFragmentManager().findFragmentByTag("rss_container_tag")!=null){
            fragmentTransaction.hide(getSupportFragmentManager().findFragmentByTag("rss_container_tag"));
        }

        if(getSupportFragmentManager().findFragmentByTag("favourites")!=null){
            fragmentTransaction.hide(getSupportFragmentManager().findFragmentByTag("favourites"));
        }

        fragmentTransaction.add(R.id.fragment_container,fragmentNewsContainer);
        fragmentTransaction.addToBackStack(null).commit();
    }

    public void populateNavigationDrawerMenu() {
        Menu menu = navigationView.getMenu();
        RSSFeedCategoryDAO rssFeedCategoryDAO = new RSSFeedCategoryDAO();
        rssFeedCategoryList = rssFeedCategoryDAO.getRSSFeedCategoryList(context);

        for (Integer i = 0; i < rssFeedCategoryList.size(); i++) {
            menu.add(R.id.navigation_drawer_menu_RSSFeedCategories, i, i, rssFeedCategoryList.get(i).getCategoryName());
            menu.setGroupCheckable(R.id.navigation_drawer_menu_RSSFeedCategories, true, true);
            menu.setGroupVisible(R.id.navigation_drawer_menu_RSSFeedCategories, true);
        }
    }

    @Override
    public void getFavNotifications(String rssFeed, FloatingActionButton fab) {

        if(favouriteListMainActivity.contains(rssFeed)){
            favouriteListMainActivity.remove(rssFeed);
            fab.setSelected(false);
            Toast.makeText(context, rssFeed + " ha sido removido de la lista de favoritos.",Toast.LENGTH_SHORT).show();
        }else{
            favouriteListMainActivity.add(rssFeed);
            Toast.makeText(context, rssFeed + " ha sido agregado de la lista de favoritos.",Toast.LENGTH_SHORT).show();
            fab.setSelected(true);
        }
        fragmentRSSFeedContainer.setFavouriteList(favouriteListMainActivity);
    }

    private class ListenerMenu implements NavigationView.OnNavigationItemSelectedListener{
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            RSSFeedCategory rssFeedCategory = rssFeedCategoryList.get(item.getItemId());

            fragmentRSSFeedContainer = new FragmentRSSFeedContainer();
            Bundle arguments = new Bundle();

            arguments.putString(FragmentRSSFeedContainer.RSSFEED_CATEGORYID, rssFeedCategory.getObjectId());
            arguments.putString(FragmentRSSFeedContainer.RSSFEED_TITLE, rssFeedCategory.getCategoryName());

            fragmentRSSFeedContainer.setArguments(arguments);
            fragmentManager = getSupportFragmentManager();

            fragmentTransaction = fragmentManager.beginTransaction().addToBackStack(null);
            fragmentTransaction.replace(R.id.fragment_container, fragmentRSSFeedContainer,"rss_container_tag");
            fragmentTransaction.commit();
            fragmentRSSFeedContainer.setFavouriteList(favouriteListMainActivity);

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(navigationView);
            }

            return true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        NewsController newsController = new NewsController();
        newsController.updateFavourites(favouriteListMainActivity,this);
    }

    public void displayFavourites(View view){

        NewsController newsController = new NewsController();
        newsController.updateFavourites(favouriteListMainActivity,context);
        List<RSSFeed> newFavouriteList = newsController.getFavouritesFromDB(context);

        if(newFavouriteList.size()>0) {
            fragmentFavouriteContainer = new FragmentFavouriteContainer();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container,fragmentFavouriteContainer,"favourites");
            fragmentTransaction.addToBackStack(null).commit();
            fragmentFavouriteContainer.setRssFeedList(newFavouriteList);
        }else{
            Toast.makeText(context, getString(R.string.favourites_rss_empty_list_warning), Toast.LENGTH_LONG).show();

        }
    }
}
