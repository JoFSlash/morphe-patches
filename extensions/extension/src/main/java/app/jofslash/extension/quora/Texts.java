package app.jofslash.extension.quora;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translations of the Patch settings texts. The English text is the key; a text that isn't listed,
 * or a phone in another language, shows the English one. Patch names stay in English, as Morphe
 * Manager and the README show them.
 */
final class Texts {

    private static final Map<String, String> SLOVAK = new HashMap<>();

    private Texts() {
    }

    /** The text in the phone's language, when there is a translation. */
    static String t(String english) {
        if (english == null) return null;
        Map<String, String> table = table();
        if (table == null) return english;
        String translated = table.get(english);
        return translated != null ? translated : english;
    }

    private static Map<String, String> table() {
        String language = Locale.getDefault().getLanguage();
        return "sk".equals(language) ? SLOVAK : null;
    }

    private static void sk(String english, String slovak) {
        SLOVAK.put(english, slovak);
    }

    static {
        // Pages, sections and buttons.
        sk("Quora patches", "Quora patche");
        sk("Privacy", "Súkromie");
        sk("Feed", "Feed");
        sk("Reading", "Čítanie");
        sk("App", "Aplikácia");
        sk("Other", "Ostatné");
        sk("More", "Ďalšie");
        sk("Back", "Späť");
        sk("Cancel", "Zrušiť");
        sk("Clear", "Vymazať");
        sk("Remove", "Odstrániť");
        sk("Copy", "Kopírovať");
        sk("Restore", "Obnoviť");
        sk("Search patches", "Hľadať patche");
        sk("Search history", "Hľadať v histórii");
        sk("Search bookmarks", "Hľadať v záložkách");
        sk("No patch matches.", "Žiadny patch nezodpovedá.");
        sk("Nothing matches.", "Nič nezodpovedá.");
        sk("Open a page in the app first; the patches that work on pages appear here once they have run.",
                "Najprv v aplikácii otvor nejakú stránku; patche, ktoré pracujú so stránkami, sa tu ukážu, keď sa spustia.");
        sk("Some changes show after restarting the app.", "Niektoré zmeny sa prejavia až po reštarte aplikácie.");
        sk("Quora patches: change them in Settings > Morphe, or long-press the Home tab.",
                "Quora patche: zmeníš ich v Nastavenia > Morphe alebo dlhým podržaním záložky Domov.");

        // More section.
        sk("Filters", "Filtre");
        sk("Keywords, authors and Spaces to hide.", "Kľúčové slová, autori a Spaces na skrytie.");
        sk("Separate with commas", "Oddeľ čiarkami");
        sk("Matching ignores case. Saved when you go back.", "Na veľkosti písmen nezáleží. Uloží sa, keď sa vrátiš späť.");
        sk("History", "História");
        sk("Answers, posts and questions you opened.", "Otvorené odpovede, príspevky a otázky.");
        sk("Bookmarks", "Záložky");
        sk("Answers and posts you saved.", "Uložené odpovede a príspevky.");
        sk("Backup", "Záloha");
        sk("Copy your settings as text, or restore them.", "Skopíruj si nastavenia ako text alebo ich obnov.");
        sk("Your settings", "Tvoje nastavenia");
        sk("Keep the text somewhere safe.", "Ulož si text niekam na bezpečné miesto.");
        sk("Paste a saved text above first.", "Najprv vlož vyššie uložený text.");
        sk("Copied.", "Skopírované.");
        sk("Settings restored.", "Nastavenia obnovené.");
        sk("That is not a saved settings text.", "Toto nie je text uložených nastavení.");
        sk("Report a problem", "Nahlásiť problém");
        sk("Copies the app details and opens a bug report on GitHub.",
                "Skopíruje údaje o aplikácii a otvorí hlásenie chyby na GitHube.");
        sk("App details copied. Paste them into the report.", "Údaje o aplikácii sú skopírované. Vlož ich do hlásenia.");
        sk("App details copied. Open github.com/JoFSlash/quora-patches/issues to report.",
                "Údaje o aplikácii sú skopírované. Problém nahlás na github.com/JoFSlash/quora-patches/issues.");

        // History and bookmarks.
        sk("Nothing yet. Answers, posts and questions you open show up here.",
                "Zatiaľ nič. Odpovede, príspevky a otázky, ktoré otvoríš, sa ukážu tu.");
        sk("Nothing saved yet. Tap the bookmark next to Share under an open answer or post.",
                "Zatiaľ nič uložené. Ťukni na záložku vedľa Share pod otvorenou odpoveďou alebo príspevkom.");
        sk("Long-press one to remove it.", "Položku odstrániš dlhým podržaním.");
        sk("Copy as text", "Kopírovať ako text");
        sk("Every title with its link, to keep or share.", "Každý názov s odkazom, na uloženie alebo zdieľanie.");
        sk("History copied.", "História skopírovaná.");
        sk("Bookmarks copied.", "Záložky skopírované.");
        sk("Clear the whole history?", "Vymazať celú históriu?");
        sk("History cleared.", "História vymazaná.");
        sk("Remove this bookmark?", "Odstrániť túto záložku?");
        sk("Saved to Bookmarks", "Uložené do záložiek");
        sk("Removed from Bookmarks", "Odstránené zo záložiek");
        sk("Just now", "Práve teraz");

        // Messages from the buttons on pages.
        sk("Answer copied", "Odpoveď skopírovaná");
        sk("Copied the preview. Open it to copy all of it.", "Skopírovaný náhľad. Celý text skopíruješ po otvorení.");
        sk("Sharing the preview. Open it to share all of it.", "Zdieľa sa náhľad. Celý text zdieľaš po otvorení.");
        sk("Translate", "Preložiť");
        sk("Translate to", "Preložiť do");
        sk("Phone's language", "Jazyk telefónu");
        sk("Slovak", "slovenčina");
        sk("Czech", "čeština");
        sk("English", "angličtina");
        sk("German", "nemčina");
        sk("Hungarian", "maďarčina");
        sk("Polish", "poľština");
        sk("Ukrainian", "ukrajinčina");
        sk("Russian", "ruština");
        sk("French", "francúzština");
        sk("Spanish", "španielčina");
        sk("Italian", "taliančina");
        sk("Portuguese", "portugalčina");
        sk("Dutch", "holandčina");
        sk("Chinese", "čínština");
        sk("Japanese", "japončina");
        sk("Korean", "kórejčina");
        sk("Arabic", "arabčina");
        sk("Hindi", "hindčina");
        sk("Turkish", "turečtina");
        sk("Saving image to Pictures/Quora", "Obrázok sa ukladá do Obrázky/Quora");
        sk("Couldn't save the image", "Obrázok sa nepodarilo uložiť");
        sk("Install MicroG-RE to sign in with Google and get notifications.",
                "Na prihlásenie cez Google a notifikácie nainštaluj MicroG-RE.");
        sk("Quora changed its pages, so some patches may not work. Long-press Home for details.",
                "Quora zmenila svoje stránky, niektoré patche nemusia fungovať. Podrobnosti po dlhom podržaní Domov.");
        sk("Page check: the feed and answers look as the patches expect.",
                "Kontrola stránok: feed aj odpovede vyzerajú tak, ako patche očakávajú.");
        sk("Page check: Quora changed its pages. These may not work: ",
                "Kontrola stránok: Quora zmenila svoje stránky. Tieto nemusia fungovať: ");
        sk(". Please report it on GitHub.", ". Nahlás to prosím na GitHube.");

        // Options.
        sk("None", "Žiadne");
        sk("Order", "Poradie");
        sk("Recent", "Najnovšie");
        sk("Upvotes", "Upvoty");
        sk("Seen items", "Videné položky");
        sk("Dim", "Stlmiť");
        sk("Hide", "Skryť");
        sk("Older than", "Staršie ako");
        sk("1 year", "1 rok");
        sk("2 years", "2 roky");
        sk("3 years", "3 roky");
        sk("5 years", "5 rokov");
        sk("10 years", "10 rokov");
        sk("Font", "Písmo");
        sk("Tabs to hide", "Skryté karty");
        sk("Tab to open on", "Úvodná karta");
        sk("Size", "Veľkosť");

        // What each switch does.
        sk("Promoted answers, sponsored posts and ad requests.", "Propagované odpovede, sponzorované príspevky a požiadavky na reklamy.");
        sk("Answers only Quora+ subscribers can read.", "Odpovede len pre predplatiteľov Quora+.");
        sk("\"Try Quora+\" buttons.", "Tlačidlá „Try Quora+“.");
        sk("Answers written by Poe bots.", "Odpovede, ktoré napísali boti Poe.");
        sk("Links and buttons that open Poe.", "Odkazy a tlačidlá, ktoré otvárajú Poe.");
        sk("Related questions on question pages.", "Súvisiace otázky na stránkach otázok.");
        sk("Questions for you to answer.", "Otázky, na ktoré máš odpovedať.");
        sk("\"Spaces you might like\".", "„Spaces you might like“.");
        sk("\"People you may know\".", "„People you may know“.");
        sk("Posts from Spaces, so the feed shows answers only.", "Príspevky zo Spaces, aby feed ukazoval len odpovede.");
        sk("Items you already opened, when they come back.", "Už otvorené položky, keď sa znova objavia.");
        sk("Answers older than the years set below.", "Odpovede staršie ako počet rokov nastavený nižšie.");
        sk("Items that mention your keywords (see Filters).", "Položky s tvojimi kľúčovými slovami (pozri Filtre).");
        sk("Items from the people and Spaces in Filters.", "Položky od ľudí a Spaces z Filtrov.");
        sk("Upvote, view, comment and share numbers.", "Počty upvotov, zobrazení, komentárov a zdieľaní.");
        sk("The downvote button.", "Tlačidlo downvote.");
        sk("\"123 comments from … and more\" rows.", "Riadky „123 comments from … and more“.");
        sk("The line under authors' names.", "Riadok pod menami autorov.");
        sk("The small Follow next to names.", "Malé Follow vedľa mien.");
        sk("\"How satisfied are you…\" and \"Was this worth your time?\" boxes.",
                "Okná „How satisfied are you…“ a „Was this worth your time?“.");
        sk("The \"What do you want to ask or share?\" box.", "Pole „What do you want to ask or share?“.");
        sk("The red \"More Stories\" button over the feed.", "Červené tlačidlo „More Stories“ nad feedom.");
        sk("The \"Add a comment…\" field under answers.", "Pole „Add a comment…“ pod odpoveďami.");
        sk("Answers to questions you marked \"Not interested\".", "Odpovede na otázky označené „Not interested“.");
        sk("Dismissed questions", "Odmietnuté otázky");
        sk("Questions whose answers the feed hides.", "Otázky, ktorých odpovede feed skrýva.");
        sk("Nothing yet. Questions you mark \"Not interested in this question\" show up here.",
                "Zatiaľ nič. Otázky označené „Not interested in this question“ sa ukážu tu.");
        sk("Show this question again?", "Znova zobrazovať túto otázku?");
        sk("Removed from the list", "Odstránené zo zoznamu");
        sk("Clear the whole list?", "Vymazať celý zoznam?");
        sk("List cleared.", "Zoznam vymazaný.");
        sk("Search dismissed questions", "Hľadať v odmietnutých otázkach");
        sk("Dismissed questions copied.", "Odmietnuté otázky skopírované.");
        sk("Answer by ", "Odpoveď od ");
        sk("Post by ", "Príspevok od ");
        sk("Answer", "Odpoveď");
        sk("Post", "Príspevok");
        sk("Question", "Otázka");
        sk("A Copy button next to Share under answers.", "Tlačidlo Kopírovať vedľa Share pod odpoveďami.");
        sk("A Share text button next to Share under answers.", "Tlačidlo Zdieľať text vedľa Share pod odpoveďami.");
        sk("Questions open with answers in the order set below.", "Otázky sa otvoria s odpoveďami v poradí nastavenom nižšie.");
        sk("Remembers what you open (see History).", "Pamätá si, čo otvoríš (pozri Históriu).");
        sk("A bookmark button under open answers (see Bookmarks).", "Tlačidlo záložky pod otvorenými odpoveďami (pozri Záložky).");
        sk("A Translate button next to Share under answers.", "Tlačidlo Preložiť vedľa Share pod odpoveďami.");
        sk("Bars hide while you scroll down.", "Lišty sa pri posúvaní nadol skryjú.");
        sk("Shortened answers open in full.", "Skrátené odpovede sa otvoria celé.");
        sk("More space between lines and paragraphs.", "Viac miesta medzi riadkami a odsekmi.");
        sk("Answers in the font set below.", "Odpovede v písme nastavenom nižšie.");
        sk("Pure black with Quora's dark theme.", "Čisto čierna s tmavou témou Quory.");
        sk("Text at the size set below. Applies after a restart.", "Text vo veľkosti nastavenej nižšie. Prejaví sa po reštarte.");
        sk("Select and copy text anywhere.", "Označ a skopíruj text kdekoľvek.");
        sk("Images load when you tap them.", "Obrázky sa načítajú až po ťuknutí.");
        sk("With Data saver, images still load on Wi-Fi.", "Pri úspore dát sa obrázky na Wi-Fi načítajú aj tak.");
        sk("Videos play only when you tap them.", "Videá sa spustia až po ťuknutí.");
        sk("The screen stays on while a page is open.", "Obrazovka ostane zapnutá, kým je otvorená stránka.");
        sk("Removes the tabs set below. Applies after a restart.", "Odstráni karty nastavené nižšie. Prejaví sa po reštarte.");
        sk("Opens on the tab set below. Applies after a restart.", "Otvorí sa na karte nastavenej nižšie. Prejaví sa po reštarte.");
        sk("Red dots and numbers on the tabs.", "Červené bodky a čísla na kartách.");
        sk("The Ask button in the top bar.", "Tlačidlo Ask v hornej lište.");
        sk("Other websites open in your browser.", "Iné weby sa otvoria v prehliadači.");
        sk("Coming back keeps the open answer and the feed.", "Po návrate ostane otvorená odpoveď aj feed.");
        sk("Pulling down doesn't reload the page. Applies after a restart.", "Potiahnutie nadol neobnoví stránku. Prejaví sa po reštarte.");
        sk("Swiping sideways doesn't change the feed. Applies after a restart.", "Potiahnutie do strany nezmení feed. Prejaví sa po reštarte.");
        sk("Other tabs load only when you open them.", "Ostatné karty sa načítajú, až keď ich otvoríš.");
        sk("No vibration when you tap buttons.", "Žiadne vibrovanie pri ťuknutí na tlačidlá.");
        sk("No \"Save password?\" or automatic sign-in.", "Žiadne „Uložiť heslo?“ ani automatické prihlásenie.");
        sk("Drops digests and promotions.", "Zahodí súhrny a propagačné notifikácie.");
    }
}
