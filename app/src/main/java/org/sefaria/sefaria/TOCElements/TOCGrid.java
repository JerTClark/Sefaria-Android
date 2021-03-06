package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.TextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.activities.MenuActivity;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.MenuElements.MenuButton;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;



public class TOCGrid extends LinearLayout {


    private Context context;
    private List<TOCNumBox> overflowButtonList;
    private List<TOCTab> TocTabList;
    private boolean hasTabs; //does current page tabs (eg with Talmud)

    //the LinearLayout which contains the actual grid of buttons, as opposed to the tabs
    //which is useful for destroying the page and switching to a new tab
    private SefariaTextView bookTitleView;
    private AutoResizeTextView currSectionTitleView;
    private LinearLayout tabRoot;
    private LinearLayout gridRoot;
    private Book book;
    private List<Node> tocNodesRoots;
    private Util.Lang lang;
    private TOCTab lastActivatedTab;

    private boolean flippedForHe;

    private double regularColumnCount = 0.0;



    public TOCGrid(Context context,Book book, List<Node> tocRoots, boolean limitGridSize, Util.Lang lang, String pathDefiningNode) {
        super(context);
        this.tocNodesRoots = tocRoots;
        this.context = context;
        //this.limitGridSize = limitGridSize;
        this.lang = lang;
        this.book = book;

        if(pathDefiningNode == null)
            pathDefiningNode = "";
        init(pathDefiningNode);
    }

    private void init(String pathDefiningNode) {
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(10, 10, 10, 100);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        this.flippedForHe = false;



        this.overflowButtonList = new ArrayList<>();
        this.TocTabList = new ArrayList<>();
        this.hasTabs = true;//lets assume for now... either with enough roots or with commentary


        bookTitleView = new SefariaTextView(context);
        bookTitleView.setFont(lang, true);
        bookTitleView.setTextSize(25);
        bookTitleView.setTextColor(Util.getColor(context, R.attr.text_color_main));
        bookTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bookTitleView.setGravity(Gravity.CENTER);
        final int bookTitlepaddding =10;
        bookTitleView.setPadding(bookTitlepaddding, bookTitlepaddding, bookTitlepaddding, bookTitlepaddding);
        this.addView(bookTitleView, 0);

        AutoResizeTextView bookCategoryView = new AutoResizeTextView(context);
        bookCategoryView.setTextColor(getResources().getColor(R.color.toc_curr_section_title));
        bookCategoryView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bookCategoryView.setText(book.getCategories());
        bookCategoryView.setTextSize(40);
        final int padding = 8;
        bookCategoryView.setPadding(padding, padding, padding, padding);
        bookCategoryView.setGravity(Gravity.CENTER);
        this.addView(bookCategoryView, 1);




        currSectionTitleView = new AutoResizeTextView(context);
        currSectionTitleView.setTextColor(getResources().getColor(R.color.toc_curr_section_title));
        currSectionTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int defaultTab = 0;
        try {
            Node node = book.getNodeFromPathStr(pathDefiningNode);
            defaultTab = node.getTocRootNum();
            String sectionTitle = node.getWholeTitle(lang); //TODO move lang to setLang
            currSectionTitleView.setText(sectionTitle);
            currSectionTitleView.setTextSize(40);

            currSectionTitleView.setPadding(padding, padding, padding, padding);
        } catch (Node.InvalidPathException e) {
            currSectionTitleView.setHeight(0);
        } catch (API.APIException e) {
            Toast.makeText(context,"Problem getting data from internet", Toast.LENGTH_SHORT).show();
        }
        currSectionTitleView.setGravity(Gravity.CENTER);
        this.addView(currSectionTitleView, 2);

        tabRoot = makeTabSections(tocNodesRoots);
        this.addView(tabRoot,3);//It's the 3nd view starting with bookTitle and CurrSectionName

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        this.addView(gridRoot, 4);

        TocTabList.get(defaultTab).setActive(true);//set it true, such that the setLang function will start the right tab


        if (getLang() == Util.Lang.HE) {
            flippedForHe = true;
            flipViews();
        }
        setLang(lang);
    }
    /*
    // /* saving method for SDK VERSION < 14 //
    private LinearLayout addRow(LinearLayout linearLayoutRoot) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        for (int i = 0; i < numColumns; i++) {
            ll.addView(new TOCNumBox(context));
        }
        linearLayoutRoot.addView(ll);
        return ll;
    }
    */


    private double getRegularColumnCount(){
        if(regularColumnCount == 0.0){
            Point size = MyApp.getScreenSize();
            regularColumnCount = (size.x)/51.0;
            if(regularColumnCount < 1.0)
                regularColumnCount = 1.0;
        }
        return regularColumnCount;
    }

    public void addNumGrid(List<Node> gridNodes,LinearLayout linearLayoutRoot, int depth) {

        //List<Integer> chaps = node.getChaps();
        if (gridNodes.size() == 0){
            Log.e("Node","Node.addNumGrid() never should have been called with 0 items");
            return;
        }




        GridLayout gl = new GridLayout(context);
        //GridLayout gl = new GridLayout(context);
        gl.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        /**
         * This is a hack to switch the order of boxes to go from right to left
         * as described in http://stackoverflow.com/questions/17830300/android-grid-view-place-items-from-right-to-left
         */
        if (lang == Util.Lang.HE) {
            //TODO make sure that this stuff still works (ei is called) when doing setLang()
            gl.setRotationY(180);
            gl.setRotationY(180);
        }

        double numColumns = getRegularColumnCount();
        if(depth > 0) depth--;
        numColumns -= (int) (depth*1.5);
        if(numColumns <1) numColumns = 1.0;

        gl.setColumnCount((int) numColumns);
        gl.setRowCount((int) Math.ceil(gridNodes.size() / numColumns));


        for (int j = 0; j < gridNodes.size(); j++) {
            //This operation of creating a new view lots of times (for example, in Araab Turim) is causing it to go really slow
            TOCNumBox tocNumBox = new TOCNumBox(context, gridNodes.get(j), lang);
            if (lang == Util.Lang.HE) {//same hack as above, such that the letters look normal
                tocNumBox.setRotationY(180);
                tocNumBox.setRotationY(180);
            }
            gl.addView(tocNumBox);
        }
        linearLayoutRoot.addView(gl);

    /*{
        //Old SDK (looks not as good, but doesn't matter as much)
        int currNodeIndex = 0;
        for (int i = 0; i <= Math.ceil(gridNodes.size() / numColumns) && currNodeIndex < gridNodes.size(); i++) {
            LinearLayout linearLayout = addRow(linearLayoutRoot);

            for (int j = 0; j < numColumns && currNodeIndex < gridNodes.size(); j++) {
                TOCNumBox tocNumBox = new TOCNumBox(context, gridNodes.get(currNodeIndex), lang);
                final int padding = 3;
                linearLayout.setPadding(padding,padding,padding,padding);
                linearLayout.addView(tocNumBox);
                currNodeIndex++;
            }
        }


    }*/
}


    private void freshGridRoot(){
        if(gridRoot != null){
            gridRoot.removeAllViews();
        }
    }

    private void activateTab(int num) {
        if(num >= TocTabList.size()){
            num = 0;
        }
        TOCTab tocTab = TocTabList.get(num);
        activateTab(tocTab);
    }

    private void activateTab(TOCTab tocTab) {
        for (TOCTab tempTocTab : TocTabList) {
            tempTocTab.setActive(false);
        }
        Log.d("TOCGrid","activating tab");
        tocTab.setActive(true);

        freshGridRoot();
        Node root = tocTab.getNode();
        if(root != null) {
            displayTree(root, gridRoot, false);
        }else{
            List<Book> commentaries = book.getAllCommentaries();
            displayCommentaries(commentaries, gridRoot);

        }
    }

    private void displayCommentaries(List<Book> commentaries, LinearLayout linearLayout){
        LinearLayout rowLinearLayout = null;
        final int columnNum = 2;
        for(int i =0;i<commentaries.size();i++){
            if((i%columnNum) == 0){
                rowLinearLayout = new LinearLayout(context);
                rowLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                //final int padding = 4;
                //rowLinearLayout.setPadding(padding,padding,padding,padding);
                if(lang == Util.Lang.HE)
                    rowLinearLayout.setGravity(Gravity.RIGHT);
                else
                    rowLinearLayout.setGravity(Gravity.LEFT);
                //TODO make it such that the hebrew goes in the right side first
                rowLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                linearLayout.addView(rowLinearLayout);
            }
            TOCCommentary tocCommentary = new TOCCommentary(context,commentaries.get(i),book,lang);
            rowLinearLayout.addView(tocCommentary);
        }
        if(commentaries.size() %columnNum != 0){ //odd number of
            TOCCommentary tocCommentary = new TOCCommentary(context,null,null,Util.Lang.EN);
            tocCommentary.setVisibility(INVISIBLE);
            rowLinearLayout.addView(tocCommentary);
        }

        return;
    }



    private void displayTree(Node node, LinearLayout linearLayout){
        displayTree(node, linearLayout, true);
    }

    private void displayTree(Node node, LinearLayout linearLayout, boolean displayLevel){
        TOCSectionName tocSectionName = new TOCSectionName(context, node, lang, displayLevel);
        linearLayout.addView(tocSectionName);
        if(lang == Util.Lang.HE) { //TODO make sure this is  still called at setLang()
            tocSectionName.setGravity(Gravity.RIGHT);
        }else {
            tocSectionName.setGravity(Gravity.LEFT);
        }
        List<Node> gridNodes = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            if(!child.isGridItem()) {
                if (gridNodes.size() > 0) {
                    //There's some gridsNodes that haven't been displayed yet
                    addNumGrid(gridNodes, tocSectionName,node.getDepth());
                    gridNodes = new ArrayList<>();
                }
                displayTree(child, tocSectionName);
            }else{
                gridNodes.add(child);
            }
        }
        if (gridNodes.size() > 0) {
            //There's some gridsNodes that haven't been displayed yet
            addNumGrid(gridNodes, tocSectionName,node.getDepth());
        }
        if(displayLevel && node.getDepth()>=2){
            tocSectionName.setDisplayingChildren(false);
        }

    }

    private LinearLayout makeTabSections(List<Node> nodeList) {
        Log.d("TOCGrid", "makeTabSections started");
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tabs.setGravity(Gravity.CENTER);


        Log.d("TOC", "nodeList.size(): " + nodeList.size());
        int numberOfTabs = nodeList.size();
        if(book.getAllCommentaries().size()>0)
            numberOfTabs++;
        for (int i=0;i<numberOfTabs;i++) {
            //ns comment from menu
            //although generally this isn't necessary b/c the nodes come from menuState.getSections
            //this is used when rebuilding after memory dump and nodes come from setHasTabs()
            //


            if(i > 0) { //skip adding the | (line) for the first item
                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                inflater.inflate(R.layout.tab_divider_menu, tabs);
            }

            TOCTab tocTab;
            if(i<nodeList.size()){
                Node node = nodeList.get(i);
                tocTab = new TOCTab(context, node, lang);
            }else {
                tocTab = new TOCTab(context, lang);//for the last one it's just commentary tab
            }
            tocTab.setOnClickListener(tabButtonClick);
            tabs.addView(tocTab);
            TocTabList.add(tocTab);

        }

        //if(numberOfTabs == 1) tabs.setVisibility(View.INVISIBLE);//removed so that people know what the structure is a bit better (seeing siman)
        return tabs;
    }



    public void setLang(Util.Lang lang) {
        this.lang = lang;
        /*
        if(lang == Util.Lang.HE){
            gridRoot.setGravity(Gravity.RIGHT);
        }else{
            gridRoot.setGravity(Gravity.LEFT);
        }*/
        gridRoot.setGravity(Gravity.CENTER);
        bookTitleView.setText(book.getTitle(lang));
        //TODO also setLang of all the Header feilds
        if ((lang == Util.Lang.HE && !flippedForHe) ||
                (lang == Util.Lang.EN && flippedForHe)) {

            flippedForHe = lang == Util.Lang.HE;
            flipViews();
        }
        for (TOCTab tempTocTab : TocTabList) {
            if(tempTocTab.getActive()){
                activateTab(tempTocTab);
            }
            tempTocTab.setLang(lang);
        }

    }

    private void flipViews() {
        if (tabRoot != null) {
            int numChildren = tabRoot.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                View tempView = tabRoot.getChildAt(numChildren - 1);
                tabRoot.removeViewAt(numChildren - 1);
                tabRoot.addView(tempView, i);
            }
        }
    }


    public Util.Lang getLang() { return lang; }


    /*
    //used when you're rebuilding after memore dump
    //you need to make sure that you add the correct tabs
    public void setHasTabs(boolean hasTabs) {
        this.hasTabs = hasTabs;
        addTabsections(tocRoots);
    }
    */
    public void goBack(boolean hasSectionBack, boolean hasTabBack) {
        //menuState.goBack(hasSectionBack, hasTabBack);

    }

    public void goHome() {
        ;//menuState.goHome();
    }

    public OnClickListener menuButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuButton mb = (MenuButton) v;
            //MenuState newMenuState = menuState.goForward(mb.getNode(), mb.getSectionNode());
            Intent intent;
            if (mb.isBook()) {
                intent = new Intent(context, TextActivity.class);
                //trick to destroy all activities beforehand
                //ComponentName cn = intent.getComponent();
                //Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
                //intent.putExtra("menuState", newMenuState)

                //jh
                //context.startActivity(intent);

            }else {
                intent = new Intent(context, MenuActivity.class);
                //intent.putExtra("menuState", newMenuState);
                intent.putExtra("hasSectionBack", mb.getSectionNode() != null);
                intent.putExtra("hasTabBack", hasTabs);


                ((Activity)context).startActivityForResult(intent, 0);
            }


        }
    };


    public OnClickListener tabButtonClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            TOCTab tocTab = (TOCTab) view;
            activateTab(tocTab);
        }
    };
}
