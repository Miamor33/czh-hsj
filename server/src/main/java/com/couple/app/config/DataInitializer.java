package com.couple.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.entity.*;
import com.couple.app.mapper.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Order(80)
public class DataInitializer implements ApplicationRunner {
    private final CoupleProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final PartnerMapper partnerMapper;
    private final SettingMapper settingMapper;
    private final AnniversaryMapper anniversaryMapper;
    private final ChallengeModuleMapper challengeModuleMapper;
    private final ChallengeItemMapper challengeItemMapper;

    public DataInitializer(CoupleProperties properties, PasswordEncoder passwordEncoder,
                           PartnerMapper partnerMapper, SettingMapper settingMapper,
                           AnniversaryMapper anniversaryMapper,
                           ChallengeModuleMapper challengeModuleMapper, ChallengeItemMapper challengeItemMapper) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.partnerMapper = partnerMapper;
        this.settingMapper = settingMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.challengeModuleMapper = challengeModuleMapper;
        this.challengeItemMapper = challengeItemMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPartners();
        seedSettings();
        seedAnniversaries();
        seedChallenges();
    }

    private void seedPartners() {
        for (Map.Entry<String, CoupleProperties.PartnerAccount> entry : properties.getPartners().entrySet()) {
            Partner existing = partnerMapper.selectOne(new LambdaQueryWrapper<Partner>()
                    .eq(Partner::getPartnerKey, entry.getKey()));
            if (existing == null) {
                Partner p = new Partner();
                p.setPartnerKey(entry.getKey());
                p.setDisplayName(entry.getValue().getDisplayName());
                p.setPasswordHash(passwordEncoder.encode(entry.getValue().getPassword()));
                p.setCreatedAt(LocalDateTime.now());
                partnerMapper.insert(p);
            }
        }
    }

    private void seedSettings() {
        if (settingMapper.selectCount(new LambdaQueryWrapper<Setting>().eq(Setting::getSettingKey, "togetherDate")) == 0) {
            Setting s = new Setting();
            s.setSettingKey("togetherDate");
            s.setSettingValue(properties.getTogetherDate());
            settingMapper.insert(s);
        }
    }

    private void seedAnniversaries() {
        if (anniversaryMapper.selectCount(null) > 0) {
            return;
        }
        Anniversary a = new Anniversary();
        a.setTitle("在一起");
        a.setEventDate(LocalDate.parse(properties.getTogetherDate()));
        a.setYearly(true);
        a.setCreatedAt(LocalDateTime.now());
        anniversaryMapper.insert(a);
    }

    private void seedChallenges() {
        if (challengeModuleMapper.selectCount(null) > 0) {
            return;
        }

        ChallengeModule little = module("little_things", "100件小事", 100, 1);
        ChallengeModule cities = module("cities_70", "走遍全国70城市", 70, 2);
        ChallengeModule tickets = module("movie_tickets", "100张电影票根", 100, 3);

        List<String> littleThings = List.of(
                "一起看一场日出", "一起看一场日落", "一起淋一场雨", "一起放风筝", "一起逛夜市",
                "一起做一顿晚饭", "一起烘焙甜点", "一起写信给对方", "一起拍拍立得", "一起骑车兜风",
                "一起看流星（或找流星）", "一起堆沙堡", "一起看海", "一起爬山", "一起露营",
                "一起唱K", "一起跳舞", "一起学一道新菜", "一起整理房间", "一起为大扫除打卡",
                "一起养一盆植物", "一起拼一幅拼图", "一起玩剧本杀", "一起看恐怖片", "一起看喜剧片",
                "一起去图书馆", "一起逛书店", "一起写愿望清单", "一起制定旅行计划", "一起做手账",
                "一起夜跑", "一起晨跑", "一起瑜伽", "一起打羽毛球", "一起游泳",
                "一起吃火锅", "一起吃寿喜烧", "一起吃路边摊", "一起尝试没吃过的菜", "一起过生日",
                "一起过纪念日", "一起跨年", "一起放孔明灯", "一起许愿", "一起看烟花",
                "一起坐摩天轮", "一起坐过山车", "一起去游乐园", "一起去动物园", "一起去博物馆",
                "一起听演唱会", "一起看话剧", "一起看展览", "一起拍情侣写真", "一起换情侣头像",
                "一起穿情侣装", "一起选礼物", "一起拆盲盒", "一起玩桌游", "一起打游戏通关",
                "一起学魔术", "一起学魔术逗对方笑", "一起学一首歌", "一起给对方按摩", "一起午睡",
                "一起看云发呆", "一起数星星", "一起听雨", "一起喝热可可", "一起泡脚聊天",
                "一起做面膜", "一起理发造型", "一起拍vlog", "一起做志愿活动", "一起捐赠闲置",
                "一起学习新技能", "一起学开车路线", "一起坐长途火车", "一起坐飞机", "一起住特色民宿",
                "一起看雪", "一起堆雪人", "一起打雪仗", "一起赏樱", "一起赏枫",
                "一起摘草莓", "一起野餐", "一起放天灯", "一起做手工", "一起折纸飞鹤",
                "一起写未来的信", "一起开盲盒惊喜", "一起完成一个小目标", "一起存一笔旅行基金", "一起制定存钱计划",
                "一起给家人准备礼物", "一起过传统节日", "一起包饺子", "一起包粽子", "一起贴春联",
                "一起做新年愿望", "一起回顾这一年", "一起拍一张封面照", "一起许下一个十年约定", "一起完成第100件小事庆祝"
        );
        insertItems(little.getId(), littleThings);

        List<String> cityList = List.of(
                "北京", "上海", "广州", "深圳", "杭州", "南京", "苏州", "成都", "重庆", "武汉",
                "西安", "长沙", "天津", "青岛", "大连", "厦门", "福州", "昆明", "贵阳", "南宁",
                "海口", "三亚", "拉萨", "乌鲁木齐", "兰州", "西宁", "银川", "呼和浩特", "哈尔滨", "长春",
                "沈阳", "石家庄", "太原", "郑州", "合肥", "南昌", "济南", "宁波", "温州", "无锡",
                "常州", "嘉兴", "绍兴", "金华", "珠海", "佛山", "东莞", "中山", "惠州", "桂林",
                "丽江", "大理", "香格里拉", "敦煌", "嘉峪关", "张家界", "凤凰古城", "洛阳", "开封", "扬州",
                "镇江", "黄山", "婺源", "景德镇", "泉州", "漳州", "汕头", "北海", "威海", "烟台"
        );
        insertItems(cities.getId(), cityList);

        for (int i = 1; i <= 100; i++) {
            ChallengeItem item = new ChallengeItem();
            item.setModuleId(tickets.getId());
            item.setTitle("第" + i + "张电影票根");
            item.setSortOrder(i);
            item.setExtraHint("填写片名与观影日期");
            challengeItemMapper.insert(item);
        }
    }

    private ChallengeModule module(String key, String title, int target, int order) {
        ChallengeModule m = new ChallengeModule();
        m.setModuleKey(key);
        m.setTitle(title);
        m.setTargetCount(target);
        m.setSortOrder(order);
        challengeModuleMapper.insert(m);
        return m;
    }

    private void insertItems(Long moduleId, List<String> titles) {
        int i = 1;
        for (String title : titles) {
            ChallengeItem item = new ChallengeItem();
            item.setModuleId(moduleId);
            item.setTitle(title);
            item.setSortOrder(i++);
            challengeItemMapper.insert(item);
        }
    }
}
