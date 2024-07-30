package velocity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

//ローマ字変換を行う
public class RomajiConversion
{
	// 設定
	private final String File = "romaji.csv";  // 読み込むファイル
	private final Path filePath;
	private final Path dataDirectory;
	// 定義を保存する変数
	private ArrayList<RomajiDefinition> RomajiDefinitionData;

	// コンストラクタ
	@Inject
	public RomajiConversion(@DataDirectory Path dataDirectory)
	{
		this.dataDirectory = dataDirectory;
		this.filePath = dataDirectory.resolve("romaji.csv");
		
		// 初期化
		RomajiDefinitionData = new ArrayList<RomajiDefinition>();
				
		try
		{
            List<String> lines = Files.readAllLines(filePath);
            
            // データの読み込み処理
            for (String s : lines)
            {
                String[] t = s.split(",");
                if (t.length == 2) RomajiDefinitionData.add(new RomajiDefinition(t[0], t[1]));
            }
            
        }
		catch (IOException e)
		{
            e.printStackTrace();
        }
		
		// データの読み込みを行う
		/*try
		{
            List<String> lines = Files.readAllLines(Paths.get(File));
            for (String s : lines)
            {
                String[] t = s.split(",");
                if (t.length == 2) RomajiDefinitionData.add(new RomajiDefinition(t[0], t[1]));
            }
        }
		catch (IOException e)
		{
            e.printStackTrace();
        }*/
	}
	
	// 変換データを保存するクラス
	private class RomajiDefinition
	{
		// 変数
		private String beforeConversion = "";
		private String afterConversion = "";
	
		// コンストラクタ
		public RomajiDefinition(String beforeConversion, String afterConversion)
		{
			this.beforeConversion = beforeConversion;
			this.afterConversion = afterConversion;
		}
		
		 // 変換前を取得
		 private String getBeforeConversion()
		 {
			 return beforeConversion;
		 }
		
		 // 変換後を取得
		 private String getAfterConversion() 
		 {
			 return afterConversion;
		 }
	}
	
	// 変換を行う関数
	public String Romaji(String data)
	{
		 // すべて小文字に
		 data = data.toLowerCase();
		
		 // 変換する
		 int i = 0;
		 do
		 {
             // 変換できるかチェック
			 for (RomajiDefinition r : RomajiDefinitionData)
			 {
				 if (data.length()-i >= r.getBeforeConversion().length()) if (data.substring(i, i+r.getBeforeConversion().length()).equals(r.getBeforeConversion()))
				 {
					 data = data.substring(0, i) + r.getAfterConversion() + data.substring(i+r.getBeforeConversion().length(), data.length());  // 変換する
					 break;
				 }
			 }
		 }
		 while (++i < data.length());  // 文字列の最後まで変換（変換があっても前の文字を調べる必要は無いはず）
		
		 return data;
	}
}
