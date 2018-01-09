package deodates.arora;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class About_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        populate_text();
    }

    private void populate_text() {

        final TextView text_about_full = (TextView)findViewById(R.id.textView_about);
        text_about_full.setText("\n[A]utonomous [R][O]ver for [R]apid [A]ssistance\n");
        text_about_full.append("\nVersion 1.0 | BUILD 2018\n");
        text_about_full.append("\n\n\n\n");
        text_about_full.append("\n\t\t\t\tProject Members:\n");
        text_about_full.append("\n\t\t\t\t\t\t\t\tJiss Joseph Thomas\n");
        text_about_full.append("\n\t\t\t\t\t\t\t\tEbin M Francis\n");
        text_about_full.append("\n\t\t\t\t\t\t\t\tReethu Elza Joseph\n");
        text_about_full.append("\n\t\t\t\t\t\t\t\tReethu Rachel Varughese\n");
        text_about_full.append("\n\nDone as part of Major Project\n");
        text_about_full.append("\nCSE 2014-18 | MBC Kuttikkanam\n");
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;
        switch (item.getItemId())
        {
            case R.id.menuitem_main:
                i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
