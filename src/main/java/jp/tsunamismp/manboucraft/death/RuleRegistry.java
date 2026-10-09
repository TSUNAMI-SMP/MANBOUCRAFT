package jp.tsunamismp.manboucraft.death;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleRegistry {
    private RuleRegistry() {}
    /** Crouching is the explicit counter to fright/stress, not to physical injury or unsafe food. */
    private static final java.util.Set<String> CALM_PROTECTED = java.util.Set.of(
            "fishing_shock", "temperature_shock", "diamond_joy", "bed_neck", "ender_pearl",
            "sprinting", "door_shock", "chest_anxiety", "tool_break", "flower_emotion",
            "bell_shock", "boat_sickness", "player_contact", "villager_stare", "heavy_bag",
            "dark_fear", "height_fear", "lava_fear", "loneliness", "nether_panic",
            "end_anxiety", "chicken_stare", "bow_recoil", "shield_shock", "bucket_panic",
            "craft_confusion", "furnace_heat", "anvil_stress", "fishing_treasure", "item_drop",
            "hand_swap", "jump_fear", "swim_shock", "bee_fear", "wolf_fear",
            "cat_judgment", "bubble_panic"
    );
    public static boolean calmProtected(String id) { return CALM_PROTECTED.contains(id); }
    public static Map<String, DeathRule> defaults() {
        List<DeathRule> rules = List.of(
            event("water_impact", "水面が思ったより硬くて絶命した。", 1),
            event("parasite", "生魚のおまけの寄生虫に敗北した。", 0.8),
            event("fishing_shock", "釣り針を見て心が釣られてしまった。", 0.35),
            event("temperature_shock", "寒暖差に身体のアップデートが追いつかなかった。", 0.4),
            event("diamond_joy", "ダイヤの輝きに心臓が追いつかなかった。", 0.5),
            event("bed_neck", "寝違えが致命傷になった。", 0.02),
            event("ender_pearl", "ワープ先に魂を置いてきた。", 0.65),
            event("sprinting", "運動不足なのに張り切りすぎた。", 0.45),
            event("egg_shock", "卵の衝撃で人生が割れた。", 0.75),
            event("snowball_shock", "雪玉を隕石と勘違いした。", 0.75),
            event("door_shock", "ドアの開閉に心の扉が耐えられなかった。", 0.02),
            event("chest_anxiety", "チェストの中身への期待で息が止まった。", 0.02),
            event("tool_break", "道具と一緒に心も折れた。", 0.9),
            event("flower_emotion", "花の美しさに命を吸い取られた。", 0.03),
            event("bell_shock", "鐘の音が心臓の終了チャイムになった。", 0.4),
            event("boat_sickness", "乗船した瞬間に船酔いの限界を迎えた。", 0.02),
            periodic("rain_stress", "雨が怖すぎて干からびた。", 0.35),
            periodic("thunder_fear", "雷が鳴る前から心臓が落ちた。", 0.4),
            periodic("player_contact", "仲間との距離感を間違えて絶命した。", 0.2),
            periodic("villager_stare", "村人からの視線に耐えられなかった。", 0.15),
            periodic("heavy_bag", "荷物の重さを考えただけで潰れた。", 0.05),
            periodic("dark_fear", "暗闇に想像力を働かせすぎた。", 0.3),
            periodic("height_fear", "高所で足より先に心が滑った。", 0.35),
            periodic("deep_water", "足が届かない事実を受け止めきれなかった。", 0.65),
            periodic("lava_fear", "溶岩の熱意に負けた。", 0.7),
            periodic("loneliness", "ひとりの時間を満喫しすぎて消えた。", 0.001),
            periodic("hunger_despair", "空腹の未来に絶望した。", 0.45),
            periodic("nether_panic", "ネザーの第一印象に耐えられなかった。", 0.0015),
            periodic("end_anxiety", "世界の果てで情緒も果てた。", 0.0015),
            periodic("chicken_stare", "ニワトリに見下されて立ち直れなかった。", 0.001),
            event("hot_food", "温かいご飯に感動して体温調節を放棄した。", 0.01),
            event("sweet_overload", "甘いものを食べて人生までとろけた。", 0.03),
            event("potion_confusion", "謎の液体を飲んで説明書の大切さを知った。", 0.015),
            event("rotten_food", "腐った肉に胃袋の敗北宣言を出された。", 0.95),
            event("suspicious_stew", "怪しいシチューを信じた自分が一番怪しかった。", 0.9),
            event("milk_shock", "牛乳の白さに未来まで真っ白になった。", 0.01),
            event("bread_choke", "パンのふわふわに喉が対応していなかった。", 0.01),
            event("bow_recoil", "弓の反動で心臓まで射出した。", 0.015),
            event("shield_shock", "盾で攻撃を防いだのに心は丸腰だった。", 0.01),
            event("bucket_panic", "バケツ一杯の責任を背負いきれなかった。", 0.02),
            event("craft_confusion", "作業台の九マスで脳の容量を使い切った。", 0.005),
            event("furnace_heat", "かまどの熱気に生命の火を消された。", 0.01),
            event("anvil_stress", "金床の請求書を見て寿命を支払った。", 0.01),
            event("fishing_treasure", "釣果を喜ぶ前に心臓が大物になった。", 0.03),
            event("item_drop", "手放したアイテムを惜しんで魂まで手放した。", 0.005),
            event("hand_swap", "持ち替えた手に脳の配線が追いつかなかった。", 0.005),
            event("jump_fear", "地面から離れた瞬間に人生からも離れた。", 0.02),
            event("sneak_cramp", "しゃがんだだけで全身がつった。", 0.001),
            event("swim_shock", "水に入って泳ぎ方より先に遺言を思い出した。", 0.35),
            event("burn_panic", "熱いと気づいた瞬間に燃え尽きた。", 0.95),
            event("cactus_prick", "小さなトゲに人生の急所を刺された。", 0.95),
            periodic("bee_fear", "怒ったハチの羽音を葬送曲と勘違いした。", 0.55),
            periodic("wolf_fear", "怒ったオオカミに心だけ先に食べられた。", 0.55),
            periodic("cat_judgment", "ネコの無言の評価に耐えられなかった。", 0.001),
            periodic("bubble_panic", "泡の勢いで魂まで浮き上がった。", 0.6)
        );
        Map<String, DeathRule> result = new LinkedHashMap<>();
        for (DeathRule rule : rules) {
            if (result.put(rule.id(), rule) != null) throw new IllegalStateException("Duplicate rule");
        }
        return result;
    }
    private static DeathRule event(String id, String message, double p) { return new DeathRule(id, message, p, 8); }
    private static DeathRule periodic(String id, String message, double p) { return new DeathRule(id, message, p, 12); }
}
