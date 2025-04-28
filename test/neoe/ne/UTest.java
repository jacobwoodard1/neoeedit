package neoe.ne;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UTest {

    @BeforeClass
    public static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }
    @Test
    public void testRunScript_invokesWithoutError() throws Exception {
        // Set up a dummy EditorPanel and PlainPage
        EditorPanel ep = new EditorPanel();
        PageData pd = PageData.newUntitled();
        PlainPage page = new PlainPage(ep, pd, null);

        ep.page = page;
        ep.frame = new JFrame(); // dummy frame for swing context

        assertDoesNotThrow(() -> {
            U.runScript(page);
        });
    }

    @Test
    public void testIsSpaceChar() {
        assertTrue(U.isSpaceChar(' '));
        assertTrue(U.isSpaceChar('\t'));
        assertFalse(U.isSpaceChar('x'));
    }

    @Test
    public void testEvalMathAndGetMathExprTail() {
        assertEquals("2", U.evalMath("1+1"));
        assertEquals("1+1", U.getMathExprTail("x=1+1"));
        assertEquals("", U.getMathExprTail("variable"));
    }

    @Test
    public void testCharAtWhenMove() {
        assertEquals('x', U.charAtWhenMove("x", 0));
        assertEquals(' ', U.charAtWhenMove("", 0));
        assertEquals(' ', U.charAtWhenMove("x", 2));
    }

    @Test
    public void testGetBool() {
        assertTrue(U.getBool("true"));
        assertTrue(U.getBool("y"));
        assertTrue(U.getBool(1));
        assertFalse(U.getBool("false"));
        assertFalse(U.getBool("0"));
    }

    @Test
    public void testGetInt() {
        assertEquals(42, U.getInt(42));
        assertEquals(42, U.getInt("42"));
        assertEquals(3, U.getInt("3.14"));
    }

    @Test
    public void testGetFloat() {
        assertEquals(42.0f, U.getFloat(42), 0.01f);
        assertEquals(3.14f, U.getFloat("3.14"), 0.01f);
    }

    @Test
    public void testTryStrWidthWithinBounds() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        Font font = new Font("Arial", Font.PLAIN, 12);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        String s = "HelloWorld";
        int width = fm.stringWidth("Hello");
        int result = U.tryStrWidth(fm, s, width, 5, 0, s.length(), 0);
        assertTrue(result >= 0 && result <= s.length());
    }

    @Test
    public void testTryStrWidthTooNarrow() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        Font font = new Font("Arial", Font.PLAIN, 12);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        String s = "HelloWorld";
        int width = fm.stringWidth("He");
        int result = U.tryStrWidth(fm, s, width, 2, 0, s.length(), 0);
        assertTrue(result >= 0 && result <= s.length());
    }

    @Test
    public void testTryStrWidthTooWide() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        Font font = new Font("Arial", Font.PLAIN, 12);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        String s = "HelloWorld";
        int width = fm.stringWidth(s) + 100;  // intentionally too wide
        int result = U.tryStrWidth(fm, s, width, s.length(), 0, s.length(), 0);
        assertEquals(s.length(), result);
    }



    // Dummy FontList implementation
    static class DummyFontList {
        public Font[] font;

        public DummyFontList(Font font) {
            this.font = new Font[] { font };
        }
    }

    @Test
    public void testOpenFileSelector() throws Exception {
        PlainPage dummyPage = new PlainPage(new EditorPanel(), PageData.newUntitled(), null);

        // No real files are needed; manual click will be required
        assertDoesNotThrow(() -> {
            U.openFileSelector("testfile.txt", dummyPage);
        });
    }

    @Test
    public void testFindPageByData() throws Exception {
        PageData data1 = PageData.newUntitled();
        PageData data2 = PageData.newUntitled();
        PlainPage page1 = new PlainPage(new EditorPanel(), data1, null);
        PlainPage page2 = new PlainPage(new EditorPanel(), data2, null);

        java.util.List<PlainPage> pages = new ArrayList<>();
        pages.add(page1);
        pages.add(page2);

        assertEquals(page1, U.findPageByData(pages, data1));
        assertEquals(page2, U.findPageByData(pages, data2));
        assertNull(U.findPageByData(pages, PageData.newUntitled())); // different data
    }

    @Test
    public void testOptimizeFileHistory() throws Exception {
        // Find where U.getFileHistoryName() would save
        File historyFile = U.getFileHistoryName();

        // Write dummy file history entries
        java.nio.file.Files.write(historyFile.toPath(), java.util.Arrays.asList(
                "file1|0:", "file2|1:", "file3|0:", "file1|0:"
        ));

        // Actually call optimize
        assertDoesNotThrow(() -> {
            try {
                U.optimizeFileHistory();
            } catch (IOException e) {
                fail("optimizeFileHistory threw IOException: " + e.getMessage());
            }
        });

        // Clean up
        historyFile.deleteOnExit();
    }


    @Test
    public void testAppendSearchResultHistory() throws Exception {
        // Create  dummy search history file manually
        File searchHistoryFile = new File(System.getProperty("user.home"), "searchhistory.txt");
        if (!searchHistoryFile.exists()) {
            searchHistoryFile.createNewFile();
        }

        java.nio.file.Files.write(searchHistoryFile.toPath(), java.util.Arrays.asList(
                "old-entry"
        ));

        assertDoesNotThrow(() -> {
            try {
                U.appendSearchResultHistory("new-search-term");
            } catch (IOException e) {
                fail("appendSearchResultHistory threw IOException: " + e.getMessage());
            }
        });

        assertTrue(searchHistoryFile.length() > 0);

        searchHistoryFile.deleteOnExit();
    }


    @Test
    public void testHardwareFailWorkaroundFilterOut() throws Exception {
        Field keymintimeField = U.class.getDeclaredField("keymintime");
        keymintimeField.setAccessible(true);
        keymintimeField.setInt(null, 1000); // 1000ms threshold

        Field keystimeField = U.class.getDeclaredField("keystime");
        keystimeField.setAccessible(true);
        Map<Integer, Long> keystime = new HashMap<>();
        keystime.put((int) 'a', System.currentTimeMillis() - 500); // 500ms ago
        keystimeField.set(null, keystime);

        KeyEvent fakeEvent = new KeyEvent(new JTextField(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');

        boolean result = U.hardwareFailWorkaroundFilterOut(fakeEvent);

        assertTrue(result, "hardwareFailWorkaroundFilterOut should detect key within time threshold");
    }

    @Test
    public void testAppendAllFileHistoryAndEpFileHistory() throws Exception {
        // Create dummy EditorPanel with a PlainPage
        EditorPanel ep = new EditorPanel();
        PageData pageData = PageData.newUntitled();
        PlainPage pp = new PlainPage(ep, pageData, null);
        ep.page = pp;

        pp.workPath = String.valueOf(new File(System.getProperty("user.dir"))); // current directory

        EditorPanel.insts.add(ep);

        assertDoesNotThrow(() -> {
            U.appendAllFileHistory();
        });

        assertDoesNotThrow(() -> {
            U.appendEpFileHistory(ep);
        });

        EditorPanel.insts.clear();
    }



}
