
package org.joinmastodon.android.utils;

import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.model.Instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MastodonLanguage {
	// On an up-to-date Mastodon instance:
	// copy(JSON.stringify(JSON.stringify(JSON.parse(document.getElementById('initial-state').textContent).languages)))
	public static String languagesJson =
			"[[\"aa\",\"Afar\",\"Afaraf\"],[\"ab\",\"Abkhaz\",\"аҧсуа бызшәа\"],[\"ae\",\"Avestan\",\"avesta\"],[\"af\",\"Afrikaans\",\"Afrikaans\"],[\"ak\",\"Akan\",\"Akan\"],[\"am\",\"Amharic\",\"አማርኛ\"],[\"an\",\"Aragonese\",\"aragonés\"],[\"ar\",\"Arabic\",\"اللغة العربية\"],[\"as\",\"Assamese\",\"অসমীয়া\"],[\"av\",\"Avaric\",\"авар мацӀ\"],[\"ay\",\"Aymara\",\"aymar aru\"],[\"az\",\"Azerbaijani\",\"azərbaycan dili\"],[\"ba\",\"Bashkir\",\"башҡорт теле\"],[\"be\",\"Belarusian\",\"беларуская мова\"],[\"bg\",\"Bulgarian\",\"български език\"],[\"bh\",\"Bihari\",\"भोजपुरी\"],[\"bi\",\"Bislama\",\"Bislama\"],[\"bm\",\"Bambara\",\"bamanankan\"],[\"bn\",\"Bengali\",\"বাংলা\"],[\"bo\",\"Tibetan\",\"བོད་ཡིག\"],[\"br\",\"Breton\",\"brezhoneg\"],[\"bs\",\"Bosnian\",\"bosanski jezik\"],[\"ca\",\"Catalan\",\"Català\"],[\"ce\",\"Chechen\",\"нохчийн мотт\"],[\"ch\",\"Chamorro\",\"Chamoru\"],[\"co\",\"Corsican\",\"corsu\"],[\"cr\",\"Cree\",\"ᓀᐦᐃᔭᐍᐏᐣ\"],[\"cs\",\"Czech\",\"čeština\"],[\"cu\",\"Old Church Slavonic\",\"ѩзыкъ словѣньскъ\"],[\"cv\",\"Chuvash\",\"чӑваш чӗлхи\"],[\"cy\",\"Welsh\",\"Cymraeg\"],[\"da\",\"Danish\",\"dansk\"],[\"de\",\"German\",\"Deutsch\"],[\"dv\",\"Divehi\",\"Dhivehi\"],[\"dz\",\"Dzongkha\",\"རྫོང་ཁ\"],[\"ee\",\"Ewe\",\"Eʋegbe\"],[\"el\",\"Greek\",\"Ελληνικά\"],[\"en\",\"English\",\"English\"],[\"eo\",\"Esperanto\",\"Esperanto\"],[\"es\",\"Spanish\",\"Español\"],[\"et\",\"Estonian\",\"eesti\"],[\"eu\",\"Basque\",\"euskara\"],[\"fa\",\"Persian\",\"فارسی\"],[\"ff\",\"Fula\",\"Fulfulde\"],[\"fi\",\"Finnish\",\"suomi\"],[\"fj\",\"Fijian\",\"Vakaviti\"],[\"fo\",\"Faroese\",\"føroyskt\"],[\"fr\",\"French\",\"Français\"],[\"fy\",\"Western Frisian\",\"Frysk\"],[\"ga\",\"Irish\",\"Gaeilge\"],[\"gd\",\"Scottish Gaelic\",\"Gàidhlig\"],[\"gl\",\"Galician\",\"galego\"],[\"gu\",\"Gujarati\",\"ગુજરાતી\"],[\"gv\",\"Manx\",\"Gaelg\"],[\"ha\",\"Hausa\",\"هَوُسَ\"],[\"he\",\"Hebrew\",\"עברית\"],[\"hi\",\"Hindi\",\"हिन्दी\"],[\"ho\",\"Hiri Motu\",\"Hiri Motu\"],[\"hr\",\"Croatian\",\"Hrvatski\"],[\"ht\",\"Haitian\",\"Kreyòl ayisyen\"],[\"hu\",\"Hungarian\",\"magyar\"],[\"hy\",\"Armenian\",\"Հայերեն\"],[\"hz\",\"Herero\",\"Otjiherero\"],[\"ia\",\"Interlingua\",\"Interlingua\"],[\"id\",\"Indonesian\",\"Bahasa Indonesia\"],[\"ie\",\"Interlingue\",\"Interlingue\"],[\"ig\",\"Igbo\",\"Asụsụ Igbo\"],[\"ii\",\"Nuosu\",\"ꆈꌠ꒿ Nuosuhxop\"],[\"ik\",\"Inupiaq\",\"Iñupiaq\"],[\"io\",\"Ido\",\"Ido\"],[\"is\",\"Icelandic\",\"Íslenska\"],[\"it\",\"Italian\",\"Italiano\"],[\"iu\",\"Inuktitut\",\"ᐃᓄᒃᑎᑐᑦ\"],[\"ja\",\"Japanese\",\"日本語\"],[\"jv\",\"Javanese\",\"basa Jawa\"],[\"ka\",\"Georgian\",\"ქართული\"],[\"kg\",\"Kongo\",\"Kikongo\"],[\"ki\",\"Kikuyu\",\"Gĩkũyũ\"],[\"kj\",\"Kwanyama\",\"Kuanyama\"],[\"kk\",\"Kazakh\",\"қазақ тілі\"],[\"kl\",\"Kalaallisut\",\"kalaallisut\"],[\"km\",\"Khmer\",\"ខេមរភាសា\"],[\"kn\",\"Kannada\",\"ಕನ್ನಡ\"],[\"ko\",\"Korean\",\"한국어\"],[\"kr\",\"Kanuri\",\"Kanuri\"],[\"ks\",\"Kashmiri\",\"कश्मीरी\"],[\"ku\",\"Kurmanji (Kurdish)\",\"Kurmancî\"],[\"kv\",\"Komi\",\"коми кыв\"],[\"kw\",\"Cornish\",\"Kernewek\"],[\"ky\",\"Kyrgyz\",\"Кыргызча\"],[\"la\",\"Latin\",\"latine\"],[\"lb\",\"Luxembourgish\",\"Lëtzebuergesch\"],[\"lg\",\"Ganda\",\"Luganda\"],[\"li\",\"Limburgish\",\"Limburgs\"],[\"ln\",\"Lingala\",\"Lingála\"],[\"lo\",\"Lao\",\"ລາວ\"],[\"lt\",\"Lithuanian\",\"lietuvių kalba\"],[\"lu\",\"Luba-Katanga\",\"Tshiluba\"],[\"lv\",\"Latvian\",\"latviešu valoda\"],[\"mg\",\"Malagasy\",\"fiteny malagasy\"],[\"mh\",\"Marshallese\",\"Kajin M̧ajeļ\"],[\"mi\",\"Māori\",\"te reo Māori\"],[\"mk\",\"Macedonian\",\"македонски јазик\"],[\"ml\",\"Malayalam\",\"മലയാളം\"],[\"mn\",\"Mongolian\",\"Монгол хэл\"],[\"mr\",\"Marathi\",\"मराठी\"],[\"ms\",\"Malay\",\"Bahasa Melayu\"],[\"mt\",\"Maltese\",\"Malti\"],[\"my\",\"Burmese\",\"ဗမာစာ\"],[\"na\",\"Nauru\",\"Ekakairũ Naoero\"],[\"nb\",\"Norwegian Bokmål\",\"Norsk bokmål\"],[\"nd\",\"Northern Ndebele\",\"isiNdebele\"],[\"ne\",\"Nepali\",\"नेपाली\"],[\"ng\",\"Ndonga\",\"Owambo\"],[\"nl\",\"Dutch\",\"Nederlands\"],[\"nn\",\"Norwegian Nynorsk\",\"Norsk Nynorsk\"],[\"no\",\"Norwegian\",\"Norsk\"],[\"nr\",\"Southern Ndebele\",\"isiNdebele\"],[\"nv\",\"Navajo\",\"Diné bizaad\"],[\"ny\",\"Chichewa\",\"chiCheŵa\"],[\"oc\",\"Occitan\",\"occitan\"],[\"oj\",\"Ojibwe\",\"ᐊᓂᔑᓈᐯᒧᐎᓐ\"],[\"om\",\"Oromo\",\"Afaan Oromoo\"],[\"or\",\"Oriya\",\"ଓଡ଼ିଆ\"],[\"os\",\"Ossetian\",\"ирон æвзаг\"],[\"pa\",\"Panjabi\",\"ਪੰਜਾਬੀ\"],[\"pi\",\"Pāli\",\"पाऴि\"],[\"pl\",\"Polish\",\"Polski\"],[\"ps\",\"Pashto\",\"پښتو\"],[\"pt\",\"Portuguese\",\"Português\"],[\"qu\",\"Quechua\",\"Runa Simi\"],[\"rm\",\"Romansh\",\"rumantsch grischun\"],[\"rn\",\"Kirundi\",\"Ikirundi\"],[\"ro\",\"Romanian\",\"Română\"],[\"ru\",\"Russian\",\"Русский\"],[\"rw\",\"Kinyarwanda\",\"Ikinyarwanda\"],[\"sa\",\"Sanskrit\",\"संस्कृतम्\"],[\"sc\",\"Sardinian\",\"sardu\"],[\"sd\",\"Sindhi\",\"सिन्धी\"],[\"se\",\"Northern Sami\",\"Davvisámegiella\"],[\"sg\",\"Sango\",\"yângâ tî sängö\"],[\"si\",\"Sinhala\",\"සිංහල\"],[\"sk\",\"Slovak\",\"slovenčina\"],[\"sl\",\"Slovenian\",\"slovenščina\"],[\"sn\",\"Shona\",\"chiShona\"],[\"so\",\"Somali\",\"Soomaaliga\"],[\"sq\",\"Albanian\",\"Shqip\"],[\"sr\",\"Serbian\",\"српски језик\"],[\"ss\",\"Swati\",\"SiSwati\"],[\"st\",\"Southern Sotho\",\"Sesotho\"],[\"su\",\"Sundanese\",\"Basa Sunda\"],[\"sv\",\"Swedish\",\"Svenska\"],[\"sw\",\"Swahili\",\"Kiswahili\"],[\"ta\",\"Tamil\",\"தமிழ்\"],[\"te\",\"Telugu\",\"తెలుగు\"],[\"tg\",\"Tajik\",\"тоҷикӣ\"],[\"th\",\"Thai\",\"ไทย\"],[\"ti\",\"Tigrinya\",\"ትግርኛ\"],[\"tk\",\"Turkmen\",\"Türkmen\"],[\"tl\",\"Tagalog\",\"Wikang Tagalog\"],[\"tn\",\"Tswana\",\"Setswana\"],[\"to\",\"Tonga\",\"faka Tonga\"],[\"tr\",\"Turkish\",\"Türkçe\"],[\"ts\",\"Tsonga\",\"Xitsonga\"],[\"tt\",\"Tatar\",\"татар теле\"],[\"tw\",\"Twi\",\"Twi\"],[\"ty\",\"Tahitian\",\"Reo Tahiti\"],[\"ug\",\"Uyghur\",\"ئۇيغۇرچە‎\"],[\"uk\",\"Ukrainian\",\"Українська\"],[\"ur\",\"Urdu\",\"اردو\"],[\"uz\",\"Uzbek\",\"Ўзбек\"],[\"ve\",\"Venda\",\"Tshivenḓa\"],[\"vi\",\"Vietnamese\",\"Tiếng Việt\"],[\"vo\",\"Volapük\",\"Volapük\"],[\"wa\",\"Walloon\",\"walon\"],[\"wo\",\"Wolof\",\"Wollof\"],[\"xh\",\"Xhosa\",\"isiXhosa\"],[\"yi\",\"Yiddish\",\"ייִדיש\"],[\"yo\",\"Yoruba\",\"Yorùbá\"],[\"za\",\"Zhuang\",\"Saɯ cueŋƅ\"],[\"zh\",\"Chinese\",\"中文\"],[\"zu\",\"Zulu\",\"isiZulu\"],[\"ast\",\"Asturian\",\"Asturianu\"],[\"ckb\",\"Sorani (Kurdish)\",\"سۆرانی\"],[\"cnr\",\"Montenegrin\",\"crnogorski\"],[\"jbo\",\"Lojban\",\"la .lojban.\"],[\"kab\",\"Kabyle\",\"Taqbaylit\"],[\"kmr\",\"Kurmanji (Kurdish)\",\"Kurmancî\"],[\"ldn\",\"Láadan\",\"Láadan\"],[\"lfn\",\"Lingua Franca Nova\",\"lingua franca nova\"],[\"sco\",\"Scots\",\"Scots\"],[\"sma\",\"Southern Sami\",\"Åarjelsaemien Gïele\"],[\"smj\",\"Lule Sami\",\"Julevsámegiella\"],[\"szl\",\"Silesian\",\"ślůnsko godka\"],[\"tai\",\"Tai\",\"ภาษาไท or ภาษาไต\"],[\"tok\",\"Toki Pona\",\"toki pona\"],[\"zba\",\"Balaibalan\",\"باليبلن\"],[\"zgh\",\"Standard Moroccan Tamazight\",\"ⵜⴰⵎⴰⵣⵉⵖⵜ\"]]";
	public static final List<MastodonLanguage> allLanguages;
	public static final MastodonLanguage ENGLISH = new MastodonLanguage("en", "English", "English");

	static {
		String[][] languages = gson.fromJson(languagesJson, String[][].class);
		allLanguages = new ArrayList<>();
		for (String[] language : languages) allLanguages.add(new MastodonLanguage(language[0], language[1], language[2]));
		Collections.sort(allLanguages, Comparator.comparing(MastodonLanguage::getDefaultName));
	}

	public static List<String> defaultRecentLanguages;

	static {
		List<Locale> systemLocales = new ArrayList<>();;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			systemLocales.add(Resources.getSystem().getConfiguration().locale);
		} else {
			LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
			for (int i = 0; i < localeList.size(); i++) systemLocales.add(localeList.get(i));
		}

		defaultRecentLanguages = systemLocales.stream()
				.map(Locale::getLanguage)
				.distinct()
				.collect(Collectors.toList());
	}

	public final String languageTag, name, englishName;
	public final Locale locale;

	private MastodonLanguage(String languageTag, String englishName, String name) {
		this.locale = new Locale(languageTag);
		this.languageTag = languageTag.toLowerCase(Locale.ROOT);
		this.name = name;
		this.englishName = englishName;
	}

	public String getDefaultName() {
		String accordingToLocale = locale.getDisplayLanguage(Locale.getDefault());
		return accordingToLocale.equals(languageTag) ? englishName : accordingToLocale;
	}

	public String getLanguageName() { return name; }

	public String getLanguage() { return languageTag; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return Objects.equals(languageTag, ((MastodonLanguage) o).languageTag);
	}

	@Override
	public int hashCode() {
		return languageTag != null ? languageTag.hashCode() : 0;
	}

	public static class LanguageResolver {
		private final MastodonLanguage fallbackLanguage;

		public LanguageResolver(Instance instanceInfo) {
			String fallbackLanguageTag = (instanceInfo.languages != null && !instanceInfo.languages.isEmpty()) ? instanceInfo.languages.get(0) : ENGLISH.languageTag;
			fallbackLanguage = allLanguages.stream()
					.filter(l->l.languageTag.equalsIgnoreCase(fallbackLanguageTag)).findAny()
					.orElse(ENGLISH);
		}

		public MastodonLanguage from(String language) {
			return allLanguages.stream()
					.filter(l->l.locale.equals(new Locale(language))).findAny()
					.orElse(fallbackLanguage);
		}

		public MastodonLanguage getDefault() {
			return from(Locale.getDefault().getLanguage());
		}
	}
}

