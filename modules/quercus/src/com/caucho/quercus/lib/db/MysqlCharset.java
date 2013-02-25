/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import java.util.HashMap;

/**
 * Metadata for mysql charsets.
 */
public class MysqlCharset
{
  private static final HashMap<String,Integer> _collationIndexMap
    = new HashMap<String,Integer>();

  private static final HashMap<String,CharsetEntry> _charsetEntryMap
    = new HashMap<String,CharsetEntry>();

  public static int getCollationIndex(String collation)
  {
    Integer index = _collationIndexMap.get(collation);

    if (index != null) {
      return index.intValue();
    }
    else {
      return 0;
    }
  }

  public static CharsetEntry getCharsetEntry(String charset)
  {
    return _charsetEntryMap.get(charset);
  }

  public static String getDefaultCollation(String charset)
  {
    CharsetEntry entry = _charsetEntryMap.get(charset);

    if (entry != null) {
      return entry.getDefaultCollation();
    }
    else {
      return "";
    }
  }

  public static String getDescription(String charset)
  {
    CharsetEntry entry = _charsetEntryMap.get(charset);

    if (entry != null) {
      return entry.getDescription();
    }
    else {
      return "";
    }
  }

  public static int getMaxBytes(String charset)
  {
    CharsetEntry entry = _charsetEntryMap.get(charset);

    if (entry != null) {
      return entry.getMaxBytes();
    }
    else {
      return 1;
    }
  }

  static class CharsetEntry
  {
    private final String _name;
    private final String _defaultCollation;
    private final String _description;
    private final int _maxBytes;

    public CharsetEntry(String name, String defaultCollation,
                        String description, int maxBytes)
    {
      _name = name;
      _defaultCollation = defaultCollation;
      _description = description;
      _maxBytes = maxBytes;
    }

    public String getName()
    {
      return _name;
    }

    public String getDefaultCollation()
    {
      return _defaultCollation;
    }

    public String getDescription()
    {
      return _description;
    }

    public int getMaxBytes()
    {
      return _maxBytes;
    }
  }

  static {
    _collationIndexMap.put("big5_chinese_ci", 1);
    _collationIndexMap.put("latin2_czech_cs", 2);
    _collationIndexMap.put("dec8_swedish_ci", 3);
    _collationIndexMap.put("cp850_general_ci", 4);
    _collationIndexMap.put("latin1_german1_ci", 5);
    _collationIndexMap.put("hp8_english_ci", 6);
    _collationIndexMap.put("koi8r_general_ci", 7);
    _collationIndexMap.put("latin1_swedish_ci", 8);
    _collationIndexMap.put("latin2_general_ci", 9);
    _collationIndexMap.put("swe7_swedish_ci", 10);
    _collationIndexMap.put("ascii_general_ci", 11);
    _collationIndexMap.put("ujis_japanese_ci", 12);
    _collationIndexMap.put("sjis_japanese_ci", 13);
    _collationIndexMap.put("cp1251_bulgarian_ci", 14);
    _collationIndexMap.put("latin1_danish_ci", 15);
    _collationIndexMap.put("hebrew_general_ci", 16);
    _collationIndexMap.put("tis620_thai_ci", 18);
    _collationIndexMap.put("euckr_korean_ci", 19);
    _collationIndexMap.put("latin7_estonian_cs", 20);
    _collationIndexMap.put("latin2_hungarian_ci", 21);
    _collationIndexMap.put("koi8u_general_ci", 22);
    _collationIndexMap.put("cp1251_ukrainian_ci", 23);
    _collationIndexMap.put("gb2312_chinese_ci", 24);
    _collationIndexMap.put("greek_general_ci", 25);
    _collationIndexMap.put("cp1250_general_ci", 26);
    _collationIndexMap.put("latin2_croatian_ci", 27);
    _collationIndexMap.put("gbk_chinese_ci", 28);
    _collationIndexMap.put("cp1257_lithuanian_ci", 29);
    _collationIndexMap.put("latin5_turkish_ci", 30);
    _collationIndexMap.put("latin1_german2_ci", 31);
    _collationIndexMap.put("armscii8_general_ci", 32);
    _collationIndexMap.put("utf8_general_ci", 33);
    _collationIndexMap.put("cp1250_czech_cs", 34);
    _collationIndexMap.put("ucs2_general_ci", 35);
    _collationIndexMap.put("cp866_general_ci", 36);
    _collationIndexMap.put("keybcs2_general_ci", 37);
    _collationIndexMap.put("macce_general_ci", 38);
    _collationIndexMap.put("macroman_general_ci", 39);
    _collationIndexMap.put("cp852_general_ci", 40);
    _collationIndexMap.put("latin7_general_ci", 41);
    _collationIndexMap.put("latin7_general_cs", 42);
    _collationIndexMap.put("macce_bin", 43);
    _collationIndexMap.put("cp1250_croatian_ci", 44);
    _collationIndexMap.put("latin1_bin", 47);
    _collationIndexMap.put("latin1_general_ci", 48);
    _collationIndexMap.put("latin1_general_cs", 49);
    _collationIndexMap.put("cp1251_bin", 50);
    _collationIndexMap.put("cp1251_general_ci", 51);
    _collationIndexMap.put("cp1251_general_cs", 52);
    _collationIndexMap.put("macroman_bin", 53);
    _collationIndexMap.put("utf16_general_ci", 54);
    _collationIndexMap.put("utf16_bin", 55);
    _collationIndexMap.put("cp1256_general_ci", 57);
    _collationIndexMap.put("cp1257_bin", 58);
    _collationIndexMap.put("cp1257_general_ci", 59);
    _collationIndexMap.put("utf32_general_ci", 60);
    _collationIndexMap.put("utf32_bin", 61);
    _collationIndexMap.put("binary", 63);
    _collationIndexMap.put("armscii8_bin", 64);
    _collationIndexMap.put("ascii_bin", 65);
    _collationIndexMap.put("cp1250_bin", 66);
    _collationIndexMap.put("cp1256_bin", 67);
    _collationIndexMap.put("cp866_bin", 68);
    _collationIndexMap.put("dec8_bin", 69);
    _collationIndexMap.put("greek_bin", 70);
    _collationIndexMap.put("hebrew_bin", 71);
    _collationIndexMap.put("hp8_bin", 72);
    _collationIndexMap.put("keybcs2_bin", 73);
    _collationIndexMap.put("koi8r_bin", 74);
    _collationIndexMap.put("koi8u_bin", 75);
    _collationIndexMap.put("latin2_bin", 77);
    _collationIndexMap.put("latin5_bin", 78);
    _collationIndexMap.put("latin7_bin", 79);
    _collationIndexMap.put("cp850_bin", 80);
    _collationIndexMap.put("cp852_bin", 81);
    _collationIndexMap.put("swe7_bin", 82);
    _collationIndexMap.put("utf8_bin", 83);
    _collationIndexMap.put("big5_bin", 84);
    _collationIndexMap.put("euckr_bin", 85);
    _collationIndexMap.put("gb2312_bin", 86);
    _collationIndexMap.put("gbk_bin", 87);
    _collationIndexMap.put("sjis_bin", 88);
    _collationIndexMap.put("tis620_bin", 89);
    _collationIndexMap.put("ucs2_bin", 90);
    _collationIndexMap.put("ujis_bin", 91);
    _collationIndexMap.put("geostd8_general_ci", 92);
    _collationIndexMap.put("geostd8_bin", 93);
    _collationIndexMap.put("latin1_spanish_ci", 94);
    _collationIndexMap.put("cp932_japanese_ci", 95);
    _collationIndexMap.put("cp932_bin", 96);
    _collationIndexMap.put("eucjpms_japanese_ci", 97);
    _collationIndexMap.put("eucjpms_bin", 98);
    _collationIndexMap.put("cp1250_polish_ci", 99);
    _collationIndexMap.put("utf16_unicode_ci", 101);
    _collationIndexMap.put("utf16_icelandic_ci", 102);
    _collationIndexMap.put("utf16_latvian_ci", 103);
    _collationIndexMap.put("utf16_romanian_ci", 104);
    _collationIndexMap.put("utf16_slovenian_ci", 105);
    _collationIndexMap.put("utf16_polish_ci", 106);
    _collationIndexMap.put("utf16_estonian_ci", 107);
    _collationIndexMap.put("utf16_spanish_ci", 108);
    _collationIndexMap.put("utf16_swedish_ci", 109);
    _collationIndexMap.put("utf16_turkish_ci", 110);
    _collationIndexMap.put("utf16_czech_ci", 111);
    _collationIndexMap.put("utf16_danish_ci", 112);
    _collationIndexMap.put("utf16_lithuanian_ci", 113);
    _collationIndexMap.put("utf16_slovak_ci", 114);
    _collationIndexMap.put("utf16_spanish2_ci", 115);
    _collationIndexMap.put("utf16_roman_ci", 116);
    _collationIndexMap.put("utf16_persian_ci", 117);
    _collationIndexMap.put("utf16_esperanto_ci", 118);
    _collationIndexMap.put("utf16_hungarian_ci", 119);
    _collationIndexMap.put("utf16_sinhala_ci", 120);
    _collationIndexMap.put("ucs2_unicode_ci", 128);
    _collationIndexMap.put("ucs2_icelandic_ci", 129);
    _collationIndexMap.put("ucs2_latvian_ci", 130);
    _collationIndexMap.put("ucs2_romanian_ci", 131);
    _collationIndexMap.put("ucs2_slovenian_ci", 132);
    _collationIndexMap.put("ucs2_polish_ci", 133);
    _collationIndexMap.put("ucs2_estonian_ci", 134);
    _collationIndexMap.put("ucs2_spanish_ci", 135);
    _collationIndexMap.put("ucs2_swedish_ci", 136);
    _collationIndexMap.put("ucs2_turkish_ci", 137);
    _collationIndexMap.put("ucs2_czech_ci", 138);
    _collationIndexMap.put("ucs2_danish_ci", 139);
    _collationIndexMap.put("ucs2_lithuanian_ci ", 140);
    _collationIndexMap.put("ucs2_slovak_ci", 141);
    _collationIndexMap.put("ucs2_spanish2_ci", 142);
    _collationIndexMap.put("ucs2_roman_ci", 143);
    _collationIndexMap.put("ucs2_persian_ci", 144);
    _collationIndexMap.put("ucs2_esperanto_ci", 145);
    _collationIndexMap.put("ucs2_hungarian_ci", 146);
    _collationIndexMap.put("utf32_unicode_ci", 160);
    _collationIndexMap.put("utf32_icelandic_ci", 161);
    _collationIndexMap.put("utf32_latvian_ci", 162);
    _collationIndexMap.put("utf32_romanian_ci", 163);
    _collationIndexMap.put("utf32_slovenian_ci", 164);
    _collationIndexMap.put("utf32_polish_ci", 165);
    _collationIndexMap.put("utf32_estonian_ci", 166);
    _collationIndexMap.put("utf32_spanish_ci", 167);
    _collationIndexMap.put("utf32_swedish_ci", 168);
    _collationIndexMap.put("utf32_turkish_ci", 169);
    _collationIndexMap.put("utf32_czech_ci", 170);
    _collationIndexMap.put("utf32_danish_ci", 171);
    _collationIndexMap.put("utf32_lithuanian_ci", 172);
    _collationIndexMap.put("utf32_slovak_ci", 173);
    _collationIndexMap.put("utf32_spanish2_ci", 174);
    _collationIndexMap.put("utf32_roman_ci", 175);
    _collationIndexMap.put("utf32_persian_ci", 176);
    _collationIndexMap.put("utf32_esperanto_ci", 177);
    _collationIndexMap.put("utf32_hungarian_ci", 178);
    _collationIndexMap.put("utf32_sinhala_ci", 179);
    _collationIndexMap.put("utf8_unicode_ci", 192);
    _collationIndexMap.put("utf8_icelandic_ci", 193);
    _collationIndexMap.put("utf8_latvian_ci", 194);
    _collationIndexMap.put("utf8_romanian_ci", 195);
    _collationIndexMap.put("utf8_slovenian_ci", 196);
    _collationIndexMap.put("utf8_polish_ci", 197);
    _collationIndexMap.put("utf8_estonian_ci", 198);
    _collationIndexMap.put("utf8_spanish_ci", 199);
    _collationIndexMap.put("utf8_swedish_ci", 200);
    _collationIndexMap.put("utf8_turkish_ci", 201);
    _collationIndexMap.put("utf8_czech_ci", 202);
    _collationIndexMap.put("utf8_danish_ci", 203);
    _collationIndexMap.put("utf8_lithuanian_ci ", 204);
    _collationIndexMap.put("utf8_slovak_ci", 205);
    _collationIndexMap.put("utf8_spanish2_ci", 206);
    _collationIndexMap.put("utf8_roman_ci", 207);
    _collationIndexMap.put("utf8_persian_ci", 208);
    _collationIndexMap.put("utf8_esperanto_ci", 209);
    _collationIndexMap.put("utf8_hungarian_ci", 210);
  }

  static {
    _charsetEntryMap.put("armscii8", new CharsetEntry("armscii8", "armscii8_general_ci", "ARMSCII-8 Armenian", 1));
    _charsetEntryMap.put("ascii", new CharsetEntry("ascii", "ascii_general_ci", "US ASCII", 1));
    _charsetEntryMap.put("big5", new CharsetEntry("big5", "big5_chinese_ci", "Big5 Traditional Chinese", 2));
    _charsetEntryMap.put("binary", new CharsetEntry("binary", "binary", "Binary pseudo charset", 1));
    _charsetEntryMap.put("cp1250", new CharsetEntry("cp1250", "cp1250_general_ci", "Windows Central European", 1));
    _charsetEntryMap.put("cp1251", new CharsetEntry("cp1251", "cp1251_general_ci", "Windows Cyrillic", 1));
    _charsetEntryMap.put("cp1256", new CharsetEntry("cp1256", "cp1256_general_ci", "Windows Arabic", 1));
    _charsetEntryMap.put("cp1257", new CharsetEntry("cp1257", "cp1257_general_ci", "Windows Baltic", 1));
    _charsetEntryMap.put("cp850", new CharsetEntry("cp850", "cp850_general_ci", "DOS West European", 1));
    _charsetEntryMap.put("cp852", new CharsetEntry("cp852", "cp852_general_ci", "DOS Central European", 1));
    _charsetEntryMap.put("cp866", new CharsetEntry("cp866", "cp866_general_ci", "DOS Russian", 1));
    _charsetEntryMap.put("cp932", new CharsetEntry("cp932", "cp932_japanese_ci", "SJIS for Windows Japanese", 2));
    _charsetEntryMap.put("dec8", new CharsetEntry("dec8", "dec8_swedish_ci", "DEC West European", 1));
    _charsetEntryMap.put("eucjpms", new CharsetEntry("eucjpms", "eucjpms_japanese_ci", "UJIS for Windows Japanese", 3));
    _charsetEntryMap.put("euckr", new CharsetEntry("euckr", "euckr_korean_ci", "EUC-KR Korean", 2));
    _charsetEntryMap.put("gb2312", new CharsetEntry("gb2312", "gb2312_chinese_ci", "GB2312 Simplified Chinese", 2));
    _charsetEntryMap.put("gbk", new CharsetEntry("gbk", "gbk_chinese_ci", "GBK Simplified Chinese", 2));
    _charsetEntryMap.put("geostd8", new CharsetEntry("geostd8", "geostd8_general_ci", "GEOSTD8 Georgian", 1));
    _charsetEntryMap.put("greek", new CharsetEntry("greek", "greek_general_ci", "ISO 8859-7 Greek", 1));
    _charsetEntryMap.put("hebrew", new CharsetEntry("hebrew", "hebrew_general_ci", "ISO 8859-8 Hebrew", 1));
    _charsetEntryMap.put("hp8", new CharsetEntry("hp8", "hp8_english_ci", "HP West European", 1));
    _charsetEntryMap.put("keybcs2", new CharsetEntry("keybcs2", "keybcs2_general_ci", "DOS Kamenicky Czech-Slovak", 1));
    _charsetEntryMap.put("koi8r", new CharsetEntry("koi8r", "koi8r_general_ci", "KOI8-R Relcom Russian", 1));
    _charsetEntryMap.put("koi8u", new CharsetEntry("koi8u", "koi8u_general_ci", "KOI8-U Ukrainian", 1));
    _charsetEntryMap.put("latin1", new CharsetEntry("latin1", "latin1_swedish_ci", "cp1252 West European", 1));
    _charsetEntryMap.put("latin2", new CharsetEntry("latin2", "latin2_general_ci", "ISO 8859-2 Central European", 1));
    _charsetEntryMap.put("latin5", new CharsetEntry("latin5", "latin5_turkish_ci", "ISO 8859-9 Turkish", 1));
    _charsetEntryMap.put("latin7", new CharsetEntry("latin7", "latin7_general_ci", "ISO 8859-13 Baltic", 1));
    _charsetEntryMap.put("macce", new CharsetEntry("macce", "macce_general_ci", "Mac Central European", 1));
    _charsetEntryMap.put("macroman", new CharsetEntry("macroman", "macroman_general_ci", "Mac West European", 1));
    _charsetEntryMap.put("sjis", new CharsetEntry("sjis", "sjis_japanese_ci", "Shift-JIS Japanese", 2));
    _charsetEntryMap.put("swe7", new CharsetEntry("swe7", "swe7_swedish_ci", "7bit Swedish", 1));
    _charsetEntryMap.put("tis620", new CharsetEntry("tis620", "tis620_thai_ci", "TIS620 Thai", 1));
    _charsetEntryMap.put("ucs2", new CharsetEntry("ucs2", "ucs2_general_ci", "UCS-2 Unicode", 2));
    _charsetEntryMap.put("ujis", new CharsetEntry("ujis", "ujis_japanese_ci", "EUC-JP Japanese", 3));
    _charsetEntryMap.put("utf16", new CharsetEntry("utf16", "utf16_general_ci", "UTF-16 Unicode", 4));
    _charsetEntryMap.put("utf32", new CharsetEntry("utf32", "utf32_general_ci", "UTF-32 Unicode", 4));
    _charsetEntryMap.put("utf8", new CharsetEntry("utf8", "utf8_general_ci", "UTF-8 Unicode", 3));
    _charsetEntryMap.put("utf8mb4", new CharsetEntry("utf8mb4", "utf8mb4_general_ci", "UTF-8 Unicode", 4));
  }
}
