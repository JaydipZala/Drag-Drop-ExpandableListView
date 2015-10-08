package com.expandlistviewdemo;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    /**
     * children items with a key and value list
     */
    private Map<String, ArrayList<String>> children;
    DragNDropListView dndListView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getData();
        dndListView = (DragNDropListView) findViewById(R.id.list_view_customizer);
        dndListView.setDragOnLongPress(true);
        dndListView.setAdapter(new DragNDropAdapter(this, new int[]{R.layout.row_item}, new int[]{R.id.txt__customizer_item}, children));
    }

    /**
     * simple function to fill the list
     */
    private void getData() {
        ArrayList<String> groups = new ArrayList<String>();
        children = Collections
                .synchronizedMap(new LinkedHashMap<String, ArrayList<String>>());
        for (int i = 0; i < 4; i++) {
            groups.add("group " + i);
        }
        for (String s : groups) {
            ArrayList<String> child = new ArrayList<String>();
            for (int i = 0; i < 4; i++) {
                child.add(s + " -value" + i);
            }
            children.put(s, child);
        }
    }
}