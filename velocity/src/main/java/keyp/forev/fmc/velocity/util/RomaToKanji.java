package keyp.forev.fmc.velocity.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class RomaToKanji {

    private static final Map<String, String> ROMA_TO_KANA_MAP = new LinkedHashMap<>();
    private final Logger logger;
	
    static {
    	ROMA_TO_KANA_MAP.put(".","。");
    	ROMA_TO_KANA_MAP.put("-","ー");
    	ROMA_TO_KANA_MAP.put(",","、");
    	ROMA_TO_KANA_MAP.put("!","！");
    	ROMA_TO_KANA_MAP.put("kkya","っきゃ");
    	ROMA_TO_KANA_MAP.put("kkyu","っきゅ");
    	ROMA_TO_KANA_MAP.put("kkyo","っきょ");
    	ROMA_TO_KANA_MAP.put("va","ヴァ");
    	ROMA_TO_KANA_MAP.put("vi","ヴィ");
    	ROMA_TO_KANA_MAP.put("vu","ヴ");
    	ROMA_TO_KANA_MAP.put("ve","ヴェ");
    	ROMA_TO_KANA_MAP.put("vo","ヴォ");
    	ROMA_TO_KANA_MAP.put("xa","ぁ");
    	ROMA_TO_KANA_MAP.put("xi","ぃ");
    	ROMA_TO_KANA_MAP.put("xu","ぅ");
    	ROMA_TO_KANA_MAP.put("xe","ぇ");
    	ROMA_TO_KANA_MAP.put("xo","ぉ");
    	ROMA_TO_KANA_MAP.put("xyo","ょ");
    	ROMA_TO_KANA_MAP.put("xya","ゃ");
    	ROMA_TO_KANA_MAP.put("xyu","ゅ");
    	ROMA_TO_KANA_MAP.put("xtu","っ");
    	ROMA_TO_KANA_MAP.put("ttya","っちゃ");
    	ROMA_TO_KANA_MAP.put("ttyu","っちゅ");
    	ROMA_TO_KANA_MAP.put("ttyo","っちょ");
    	ROMA_TO_KANA_MAP.put("ssya","っしゃ");
    	ROMA_TO_KANA_MAP.put("ssha","っしゃ");
    	ROMA_TO_KANA_MAP.put("ssyu","っしゅ");
    	ROMA_TO_KANA_MAP.put("sshu","っしゅ");
    	ROMA_TO_KANA_MAP.put("ssho","っしょ");
    	ROMA_TO_KANA_MAP.put("ssyo","っしょ");

    	ROMA_TO_KANA_MAP.put("tye","ちぇ");

    	ROMA_TO_KANA_MAP.put("be11a","ベラ");
    	ROMA_TO_KANA_MAP.put("BELLA","ベラ");
    	ROMA_TO_KANA_MAP.put("Bella","ベラ");
    	ROMA_TO_KANA_MAP.put("bella","ベラ");
    	ROMA_TO_KANA_MAP.put("ggo","っご");
    	ROMA_TO_KANA_MAP.put("she","しぇ");
    	ROMA_TO_KANA_MAP.put("ttya","っちゃ");
    	ROMA_TO_KANA_MAP.put("nye","にぇ");
    	ROMA_TO_KANA_MAP.put("di","ぢ");
    	ROMA_TO_KANA_MAP.put("ssi","っし");
    	ROMA_TO_KANA_MAP.put("fa","ふぁ");
    	ROMA_TO_KANA_MAP.put("fa","ふぁ");
    	ROMA_TO_KANA_MAP.put("fi","ふぃ");
    	ROMA_TO_KANA_MAP.put("fe","ふぇ");
    	ROMA_TO_KANA_MAP.put("fo","ふぉ");
    	ROMA_TO_KANA_MAP.put("ddo","っど");
    	ROMA_TO_KANA_MAP.put("gya","ぎゃ");
    	ROMA_TO_KANA_MAP.put("ggu","っぐ");
    	ROMA_TO_KANA_MAP.put("wwa","っわ");
    	ROMA_TO_KANA_MAP.put("zya","じゃ");
    	ROMA_TO_KANA_MAP.put("zyu","じゅ");
    	ROMA_TO_KANA_MAP.put("zyo","じょ");

    	ROMA_TO_KANA_MAP.put("zzya","っじゃ");
    	ROMA_TO_KANA_MAP.put("zyu","じゅ");
    	ROMA_TO_KANA_MAP.put("zyo","じょ");

    	ROMA_TO_KANA_MAP.put("zza","っざ");
    	ROMA_TO_KANA_MAP.put("zzi","っじ");
    	ROMA_TO_KANA_MAP.put("zzu","っず");
    	ROMA_TO_KANA_MAP.put("zze","っぜ");
    	ROMA_TO_KANA_MAP.put("zzo","っぞ");

    	ROMA_TO_KANA_MAP.put("hha","っは");
    	ROMA_TO_KANA_MAP.put("hhi","っひ");
    	ROMA_TO_KANA_MAP.put("ffu","っふ");
    	ROMA_TO_KANA_MAP.put("hhu","っふ");
    	ROMA_TO_KANA_MAP.put("hhe","っへ");
    	ROMA_TO_KANA_MAP.put("hho","っほ");

    	ROMA_TO_KANA_MAP.put("du","づ");
    	ROMA_TO_KANA_MAP.put("shu","しゅ");
    	ROMA_TO_KANA_MAP.put("dya","ぢゃ");
    	ROMA_TO_KANA_MAP.put("dyu","ぢゅ");
    	ROMA_TO_KANA_MAP.put("dyo","ぢょ");

    	ROMA_TO_KANA_MAP.put("yya","っや");

    	ROMA_TO_KANA_MAP.put("hu","ふ");
    	ROMA_TO_KANA_MAP.put("la","ぁ");
    	ROMA_TO_KANA_MAP.put("li","ぃ");
    	ROMA_TO_KANA_MAP.put("lu","ぅ");
    	ROMA_TO_KANA_MAP.put("le","ぇ");
    	ROMA_TO_KANA_MAP.put("lo","ぉ");
    	ROMA_TO_KANA_MAP.put("lya","ゃ");
    	ROMA_TO_KANA_MAP.put("lyu","ゅ");
    	ROMA_TO_KANA_MAP.put("lyo","ょ");
    	ROMA_TO_KANA_MAP.put("we","うぇ");
    	ROMA_TO_KANA_MAP.put("sya","しゃ");
    	ROMA_TO_KANA_MAP.put("syu","しゅ");
    	ROMA_TO_KANA_MAP.put("shu","しゅ");
    	ROMA_TO_KANA_MAP.put("syo","しょ");
    	ROMA_TO_KANA_MAP.put("kka","っか");
    	ROMA_TO_KANA_MAP.put("kki","っき");
    	ROMA_TO_KANA_MAP.put("kku","っく");
    	ROMA_TO_KANA_MAP.put("kke","っけ");
    	ROMA_TO_KANA_MAP.put("kko","っこ");
    	ROMA_TO_KANA_MAP.put("ssa","っさ");
    	ROMA_TO_KANA_MAP.put("sshi","っし");
    	ROMA_TO_KANA_MAP.put("ssu","っす");
    	ROMA_TO_KANA_MAP.put("sse","っせ");
    	ROMA_TO_KANA_MAP.put("sso","っそ");
    	ROMA_TO_KANA_MAP.put("tta","った");
    	ROMA_TO_KANA_MAP.put("tti","っち");
    	ROMA_TO_KANA_MAP.put("ttu","っつ");
    	ROMA_TO_KANA_MAP.put("tte","って");
    	ROMA_TO_KANA_MAP.put("tto","っと");
    	ROMA_TO_KANA_MAP.put("ppa","っぱ");
    	ROMA_TO_KANA_MAP.put("ppi","っぴ");
    	ROMA_TO_KANA_MAP.put("ppu","っぷ");
    	ROMA_TO_KANA_MAP.put("ppe","っぺ");
    	ROMA_TO_KANA_MAP.put("ppo","っぽ");
    	ROMA_TO_KANA_MAP.put("ga","が");
    	ROMA_TO_KANA_MAP.put("gi","ぎ");
    	ROMA_TO_KANA_MAP.put("gu","ぐ");
    	ROMA_TO_KANA_MAP.put("ge","げ");
    	ROMA_TO_KANA_MAP.put("go","ご");
    	ROMA_TO_KANA_MAP.put("za","ざ");
    	ROMA_TO_KANA_MAP.put("zi","じ");
    	ROMA_TO_KANA_MAP.put("ji","じ");
    	ROMA_TO_KANA_MAP.put("zu","ず");
    	ROMA_TO_KANA_MAP.put("ze","ぜ");
    	ROMA_TO_KANA_MAP.put("zo","ぞ");
    	ROMA_TO_KANA_MAP.put("da","だ");
    	ROMA_TO_KANA_MAP.put("ji","じ");
    	ROMA_TO_KANA_MAP.put("zu","づ");
    	ROMA_TO_KANA_MAP.put("de","で");
    	ROMA_TO_KANA_MAP.put("do","ど");
    	ROMA_TO_KANA_MAP.put("ba","ば");
    	ROMA_TO_KANA_MAP.put("bi","び");
    	ROMA_TO_KANA_MAP.put("bu","ぶ");
    	ROMA_TO_KANA_MAP.put("be","べ");
    	ROMA_TO_KANA_MAP.put("bo","ぼ");
    	ROMA_TO_KANA_MAP.put("pa","ぱ");
    	ROMA_TO_KANA_MAP.put("pi","ぴ");
    	ROMA_TO_KANA_MAP.put("pu","ぷ");
    	ROMA_TO_KANA_MAP.put("pe","ぺ");
    	ROMA_TO_KANA_MAP.put("po","ぽ");
    	ROMA_TO_KANA_MAP.put("kya","きゃ");
    	ROMA_TO_KANA_MAP.put("kyu","きゅ");
    	ROMA_TO_KANA_MAP.put("kyo","きょ");
    	ROMA_TO_KANA_MAP.put("sha","しゃ");
    	ROMA_TO_KANA_MAP.put("shu","しゅ");
    	ROMA_TO_KANA_MAP.put("sho","しょ");
    	ROMA_TO_KANA_MAP.put("tya","ちゃ");
    	ROMA_TO_KANA_MAP.put("tyu","ちゅ");
    	ROMA_TO_KANA_MAP.put("tyo","ちょ");
    	ROMA_TO_KANA_MAP.put("cha","ちゃ");
    	ROMA_TO_KANA_MAP.put("chu","ちゅ");
    	ROMA_TO_KANA_MAP.put("cho","ちょ");
    	ROMA_TO_KANA_MAP.put("nya","にゃ");
    	ROMA_TO_KANA_MAP.put("nyu","にゅ");
    	ROMA_TO_KANA_MAP.put("nyo","にょ");
    	ROMA_TO_KANA_MAP.put("hya","ひゃ");
    	ROMA_TO_KANA_MAP.put("hyu","ひゅ");
    	ROMA_TO_KANA_MAP.put("hyo","ひょ");
    	ROMA_TO_KANA_MAP.put("mya","みゃ");
    	ROMA_TO_KANA_MAP.put("myu","みゅ");
    	ROMA_TO_KANA_MAP.put("myo","みょ");
    	ROMA_TO_KANA_MAP.put("rya","りゃ");
    	ROMA_TO_KANA_MAP.put("ryu","りゅ");
    	ROMA_TO_KANA_MAP.put("ryo","りょ");
    	ROMA_TO_KANA_MAP.put("gya","ぎゃ");
    	ROMA_TO_KANA_MAP.put("gyu","ぎゅ");
    	ROMA_TO_KANA_MAP.put("gyo","ぎょ");
    	ROMA_TO_KANA_MAP.put("ja","じゃ");
    	ROMA_TO_KANA_MAP.put("jya","じゃ");
    	ROMA_TO_KANA_MAP.put("ju","じゅ");
    	ROMA_TO_KANA_MAP.put("jyu","じゅ");
    	ROMA_TO_KANA_MAP.put("jo","じょ");
    	ROMA_TO_KANA_MAP.put("jyo","じょ");
    	ROMA_TO_KANA_MAP.put("bya","びゃ");
    	ROMA_TO_KANA_MAP.put("byu","びゅ");
    	ROMA_TO_KANA_MAP.put("byo","びょ");
    	ROMA_TO_KANA_MAP.put("pya","ぴゃ");
    	ROMA_TO_KANA_MAP.put("pyu","ぴゅ");
    	ROMA_TO_KANA_MAP.put("pyo","ぴょ");
    	ROMA_TO_KANA_MAP.put("chi","ち");
    	ROMA_TO_KANA_MAP.put("tsu","つ");
    	ROMA_TO_KANA_MAP.put("ka","か");
    	ROMA_TO_KANA_MAP.put("ki","き");
    	ROMA_TO_KANA_MAP.put("ku","く");
    	ROMA_TO_KANA_MAP.put("ke","け");
    	ROMA_TO_KANA_MAP.put("ko","こ");
    	ROMA_TO_KANA_MAP.put("sa","さ");
    	ROMA_TO_KANA_MAP.put("shi","し");
    	ROMA_TO_KANA_MAP.put("si","し");
    	ROMA_TO_KANA_MAP.put("su","す");
    	ROMA_TO_KANA_MAP.put("se","せ");
    	ROMA_TO_KANA_MAP.put("so","そ");
    	ROMA_TO_KANA_MAP.put("ta","た");
    	ROMA_TO_KANA_MAP.put("ti","ち");
    	ROMA_TO_KANA_MAP.put("tu","つ");
    	ROMA_TO_KANA_MAP.put("te","て");
    	ROMA_TO_KANA_MAP.put("to","と");
    	ROMA_TO_KANA_MAP.put("na","な");
    	ROMA_TO_KANA_MAP.put("ni","に");
    	ROMA_TO_KANA_MAP.put("nu","ぬ");
    	ROMA_TO_KANA_MAP.put("ne","ね");
    	ROMA_TO_KANA_MAP.put("no","の");
    	ROMA_TO_KANA_MAP.put("ha","は");
    	ROMA_TO_KANA_MAP.put("hi","ひ");
    	ROMA_TO_KANA_MAP.put("fu","ふ");
    	ROMA_TO_KANA_MAP.put("he","へ");
    	ROMA_TO_KANA_MAP.put("ho","ほ");
    	ROMA_TO_KANA_MAP.put("ma","ま");
    	ROMA_TO_KANA_MAP.put("mi","み");
    	ROMA_TO_KANA_MAP.put("mu","む");
    	ROMA_TO_KANA_MAP.put("me","め");
    	ROMA_TO_KANA_MAP.put("mo","も");
    	ROMA_TO_KANA_MAP.put("ra","ら");
    	ROMA_TO_KANA_MAP.put("ri","り");
    	ROMA_TO_KANA_MAP.put("ru","る");
    	ROMA_TO_KANA_MAP.put("re","れ");
    	ROMA_TO_KANA_MAP.put("ro","ろ");
    	ROMA_TO_KANA_MAP.put("wa","わ");
    	ROMA_TO_KANA_MAP.put("wo","を");
    	ROMA_TO_KANA_MAP.put("ya","や");
    	ROMA_TO_KANA_MAP.put("yu","ゆ");
    	ROMA_TO_KANA_MAP.put("yo","よ");
    	ROMA_TO_KANA_MAP.put("a","あ");
    	ROMA_TO_KANA_MAP.put("i","い");
    	ROMA_TO_KANA_MAP.put("u","う");
    	ROMA_TO_KANA_MAP.put("e","え");
    	ROMA_TO_KANA_MAP.put("o","お");
    	ROMA_TO_KANA_MAP.put("nn","ん");
    	ROMA_TO_KANA_MAP.put("n","ん");
    	ROMA_TO_KANA_MAP.put("m","ん");
    }

	@Inject
	public RomaToKanji(Logger logger) {
		this.logger = logger;
	}

    public String ConvRomaToKana(String word) {
    	// Map.Entryをリストに変換して逆順にソート
        List<Map.Entry<String, String>> entries = new ArrayList<>(ROMA_TO_KANA_MAP.entrySet());
        //Collections.reverse(entries);

        // 逆順で変換を適用
        for (Map.Entry<String, String> entry : entries) {
            word = word.replace(entry.getKey(), entry.getValue());
        }

        return word;
    }

    public String ConvRomaToKanji(String kana) {
        try {
            String urlStr = "http://www.google.com/transliterate?langpair=ja-Hira%7Cja&text=" + URLEncoder.encode(kana, StandardCharsets.UTF_8);

            // URI オブジェクトを作成
            URI uri = new URI(urlStr);

            // HttpClientを使用してリクエストを作成し送信する
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

            // レスポンスを取得
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // JSONレスポンスから漢字を解析する
            String kanji = parseKanjiFromJson(responseBody);
            return kanji;
        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.error("An IOException | InterruptedException | URISyntaxException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            return "";
        }
    }

    private String parseKanjiFromJson(String json) {
        StringBuilder kanji = new StringBuilder();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONArray subArray = array.getJSONArray(i);
                if (subArray.length() > 1) {
                    org.json.JSONArray kanjiArray = subArray.getJSONArray(1);
                    if (kanjiArray.length() > 0) {
                        kanji.append(kanjiArray.getString(0));
                    }
                }
            }
        } catch (org.json.JSONException e) {
        }

        return kanji.toString();
    }
}