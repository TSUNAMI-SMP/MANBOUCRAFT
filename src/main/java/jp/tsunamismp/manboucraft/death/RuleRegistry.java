package jp.tsunamismp.manboucraft.death;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleRegistry {
    private RuleRegistry() {}
    public static Map<String, DeathRule> defaults() {
        List<DeathRule> rules = List.of(
            event("water_impact", "水面が思ったより硬くて絶命した。", .20),
            event("parasite", "生魚のおまけの寄生虫に敗北した。", .05),
            event("fishing_shock", "釣り針を見て心が釣られてしまった。", .05),
            event("temperature_shock", "寒暖差に身体のアップデートが追いつかなかった。", .05),
            event("diamond_joy", "ダイヤの輝きに心臓が追いつかなかった。", .05),
            event("bed_neck", "寝違えが致命傷になった。", .03),
            event("ender_pearl", "ワープ先に魂を置いてきた。", .10),
            event("sprinting", "運動不足なのに張り切りすぎた。", .05),
            event("egg_shock", "卵の衝撃で人生が割れた。", .05),
            event("snowball_shock", "雪玉を隕石と勘違いした。", .05),
            event("door_shock", "ドアの開閉に心の扉が耐えられなかった。", .02),
            event("chest_anxiety", "チェストの中身への期待で息が止まった。", .02),
            event("tool_break", "道具と一緒に心も折れた。", .10),
            event("flower_emotion", "花の美しさに命を吸い取られた。", .02),
            event("bell_shock", "鐘の音が心臓の終了チャイムになった。", .05),
            event("boat_sickness", "乗船した瞬間に船酔いの限界を迎えた。", .03),
            periodic("rain_stress", "雨が怖すぎて干からびた。", .01),
            periodic("thunder_fear", "雷が鳴る前から心臓が落ちた。", .02),
            periodic("player_contact", "仲間との距離感を間違えて絶命した。", .02),
            periodic("villager_stare", "村人からの視線に耐えられなかった。", .02),
            periodic("heavy_bag", "荷物の重さを考えただけで潰れた。", .01),
            periodic("dark_fear", "暗闇に想像力を働かせすぎた。", .01),
            periodic("height_fear", "高所で足より先に心が滑った。", .01),
            periodic("deep_water", "足が届かない事実を受け止めきれなかった。", .02),
            periodic("lava_fear", "溶岩の熱意に負けた。", .02),
            periodic("loneliness", "ひとりの時間を満喫しすぎて消えた。", .005),
            periodic("hunger_despair", "空腹の未来に絶望した。", .01),
            periodic("nether_panic", "ネザーの第一印象に耐えられなかった。", .01),
            periodic("end_anxiety", "世界の果てで情緒も果てた。", .01),
            periodic("chicken_stare", "ニワトリに見下されて立ち直れなかった。", .01)
        );
        Map<String, DeathRule> result = new LinkedHashMap<>();
        for (DeathRule rule : rules) {
            if (result.put(rule.id(), rule) != null) throw new IllegalStateException("Duplicate rule");
        }
        return result;
    }
    private static DeathRule event(String id, String message, double p) { return new DeathRule(id, message, p, 30); }
    private static DeathRule periodic(String id, String message, double p) { return new DeathRule(id, message, p, 60); }
}
