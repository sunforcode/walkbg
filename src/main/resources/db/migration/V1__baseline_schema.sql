-- V1__baseline_schema.sql
-- walkbg 数据库基线结构
-- 由 Hibernate schema-generation 从当前 JPA 实体导出，
-- 并与本地 walkbg 库逐列比对验证一致（53 表 / 460 列，零差异）。
--
-- 注意：此文件是基线，请勿修改。后续表结构变更请新建 V2__xxx.sql。

-- ============ 表结构 ============

create table campsites (
        campsite_type integer not null,
        elevation float(53),
        latitude float(53),
        longitude float(53),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        last_verified_id varchar(64),
        route_id varchar(64) not null,
        name varchar(200) not null,
        description TEXT,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table contacts (
        is_verified bit not null,
        price decimal(10,2),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        phone varchar(20) not null,
        created_by varchar(64),
        id varchar(64) not null,
        route_id varchar(64) not null,
        updated_by varchar(64),
        verified_by varchar(64),
        name varchar(100) not null,
        location varchar(200),
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table daily_plan_segments (
        sequence_number integer not null,
        daily_plan_id varchar(64) not null,
        id varchar(64) not null,
        segment_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table daily_plans (
        day_number integer not null,
        distance float(53),
        elevation_gain integer,
        elevation_loss float(53),
        estimated_time float(53),
        max_elevation float(53),
        min_elevation float(53),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        accommodation varchar(200),
        title varchar(200) not null,
        description TEXT,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table equipment_items (
        category integer not null,
        quantity integer not null,
        weight decimal(8,2) not null,
        weight_unit integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        name varchar(200) not null,
        primary key (id)
    ) engine=InnoDB;

create table equipment_list_items (
        quantity integer not null,
        equipment_item_id varchar(64) not null,
        equipment_list_id varchar(64) not null,
        notes TEXT,
        primary key (equipment_item_id, equipment_list_id)
    ) engine=InnoDB;

create table equipment_lists (
        person_count integer not null,
        status integer not null,
        total_weight decimal(8,2) not null,
        type integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        creator_id varchar(64),
        id varchar(64) not null,
        trip_id varchar(64),
        name varchar(200) not null,
        primary key (id)
    ) engine=InnoDB;

create table equipment_templates (
        category integer not null,
        is_official bit not null,
        rating float(53) not null,
        type integer not null,
        usage_count integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        creator_id varchar(255),
        creator_name varchar(255),
        description TEXT,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table food_items (
        calories float(27),
        carbs float(27),
        fat float(27),
        is_owned bit not null,
        prepared bit not null,
        price float(34),
        protein float(27),
        quantity integer not null,
        weight float(27),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        name varchar(200) not null,
        description TEXT,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table guides (
        like_count integer not null,
        status integer not null,
        view_count integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        author_id varchar(64) not null,
        id varchar(64) not null,
        title varchar(200) not null,
        cover_url varchar(500),
        tags varchar(500),
        content TEXT,
        primary key (id)
    ) engine=InnoDB;

create table hitchhike_contacts (
        last_verified bit not null,
        price float(53),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        phone varchar(20) not null,
        created_by varchar(64),
        id varchar(64) not null,
        route_id varchar(64) not null,
        name varchar(100) not null,
        location varchar(200),
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table marker_points (
        elevation float(53),
        latitude float(53),
        longitude float(53),
        marker_type integer not null,
        color varchar(7),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        name varchar(200),
        icon_url varchar(500),
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table meal_days (
        day_number integer not null,
        id varchar(64) not null,
        meal_plan_id varchar(64) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table meal_food_items (
        quantity integer not null,
        meal_type varchar(20) not null,
        food_item_id varchar(64) not null,
        id varchar(64) not null,
        meal_day_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table meal_items (
        meal_type integer not null,
        quantity integer not null,
        id varchar(64) not null,
        meal_day_id varchar(64) not null,
        food_name varchar(100) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table meal_plan_tags (
        tag varchar(50) not null,
        id varchar(64) not null,
        meal_plan_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table meal_plans (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        trip_id varchar(64),
        name varchar(200) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table path_points (
        distance_from_start float(53),
        elevation float(53),
        latitude float(53) not null,
        longitude float(53) not null,
        sequence_number integer not null,
        created_at datetime(6) not null,
        timestamp datetime(6),
        updated_at datetime(6) not null,
        point_type varchar(50),
        type varchar(50),
        id varchar(64) not null,
        segment_id varchar(64) not null,
        name varchar(200),
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table poi_points (
        confidence float(53),
        elevation float(53),
        latitude float(53) not null,
        longitude float(53) not null,
        created_at datetime(6) not null,
        category varchar(32) not null,
        source varchar(32) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        sub_category varchar(64),
        name varchar(200) not null,
        card_data TEXT,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table route_contacts (
        contact_type integer not null,
        priority integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        contact_id varchar(64) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table route_facilities (
        id varchar(64) not null,
        route_id varchar(64) not null,
        accommodation TEXT,
        food TEXT,
        signal_coverage TEXT,
        toilets TEXT,
        water TEXT,
        primary key (id)
    ) engine=InnoDB;

create table route_images (
        is_cover bit not null,
        sequence_number integer not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        image_url varchar(500) not null,
        primary key (id)
    ) engine=InnoDB;

create table route_map_data (
        altitude decimal(38,2),
        distance decimal(38,2),
        duration integer,
        elevation_gain decimal(38,2),
        elevation_loss decimal(38,2),
        latitude decimal(38,2),
        longitude decimal(38,2),
        completion_count bigint not null,
        created_at datetime(6) not null,
        favorite_count bigint not null,
        trip_count bigint not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        gpx_url varchar(500),
        kml_url varchar(500),
        primary key (id)
    ) engine=InnoDB;

create table route_ratings (
        difficulty float(53),
        experience float(53),
        facilities float(53),
        overall float(53),
        rating_count integer not null,
        scenery float(53),
        id varchar(64) not null,
        route_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table route_tags (
        tag varchar(50) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table route_weather (
        id varchar(64) not null,
        route_id varchar(64) not null,
        best_seasons TEXT,
        description TEXT,
        precautions TEXT,
        primary key (id)
    ) engine=InnoDB;

create table routes (
        difficulty integer,
        is_favorite bit not null,
        is_loop bit not null,
        popularity integer not null,
        route_type integer,
        status integer not null,
        usage_count integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        created_by varchar(64) not null,
        default_map_id varchar(64),
        id varchar(64) not null,
        region_id varchar(64),
        region varchar(100),
        name varchar(200) not null,
        cover_url varchar(500),
        description TEXT,
        image_urls TEXT,
        primary key (id)
    ) engine=InnoDB;

create table seasonal_weather (
        season varchar(50) not null,
        id varchar(64) not null,
        route_weather_id varchar(64) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table segment_closures (
        created_at datetime(6) not null,
        end_date datetime(6),
        start_date datetime(6),
        updated_at datetime(6) not null,
        closure_type varchar(50),
        id varchar(64) not null,
        segment_id varchar(64) not null,
        reason TEXT,
        primary key (id)
    ) engine=InnoDB;

create table segment_hazards (
        severity_level integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id varchar(64) not null,
        segment_id varchar(64) not null,
        hazard varchar(100) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table segment_keypoints (
        sequence_number integer not null,
        id varchar(64) not null,
        segment_id varchar(64),
        waypoint_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table segment_schemes (
        is_default bit not null,
        created_at datetime(6) not null,
        scheme_type varchar(32) not null,
        id varchar(64) not null,
        label varchar(64) not null,
        route_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table segments (
        difficulty integer,
        distance float(53),
        elevation_gain float(53),
        elevation_loss float(53),
        estimated_time float(53),
        route_type integer,
        sequence_number integer,
        track_end_index integer,
        track_start_index integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        color varchar(20),
        scheme_type varchar(32),
        end_point_id varchar(64),
        id varchar(64) not null,
        route_id varchar(64) not null,
        scheme_id varchar(64),
        start_point_id varchar(64),
        name varchar(200) not null,
        description TEXT,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table supplies (
        elevation float(53),
        latitude float(53),
        longitude float(53),
        supply_type integer,
        created_at datetime(6) not null,
        last_verified_at datetime(6),
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        last_verified varchar(64),
        route_id varchar(64) not null,
        updated_by varchar(64),
        name varchar(200) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table template_equipment_items (
        category integer not null,
        is_shared bit not null,
        necessity integer not null,
        quantity integer not null,
        shared_person_count integer,
        weight float(53) not null,
        weight_unit integer not null,
        template_id varchar(64),
        brand varchar(255),
        description TEXT,
        id varchar(255) not null,
        image_url varchar(255),
        model varchar(255),
        name varchar(255) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table template_seasons (
        season integer not null,
        id bigint not null auto_increment,
        template_id varchar(64),
        primary key (id)
    ) engine=InnoDB;

create table template_tags (
        id bigint not null auto_increment,
        template_id varchar(64),
        tag varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table trip_contacts (
        contact_type integer not null,
        priority integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        contact_id varchar(64) not null,
        id varchar(64) not null,
        trip_id varchar(64) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table trip_images (
        is_cover bit not null,
        sequence_number integer not null,
        id varchar(64) not null,
        trip_id varchar(64) not null,
        image_url varchar(500) not null,
        primary key (id)
    ) engine=InnoDB;

create table trip_itinerary (
        day_number integer not null,
        distance float(27),
        elevation_gain float(27),
        elevation_loss float(27),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        duration varchar(50),
        end_waypoint_id varchar(64),
        id varchar(64) not null,
        start_waypoint_id varchar(64),
        trip_id varchar(64) not null,
        accommodation varchar(200),
        title varchar(200) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table trip_participants (
        role integer not null,
        status integer not null,
        joined_at datetime(6) not null,
        trip_id varchar(64) not null,
        user_id varchar(64) not null,
        primary key (trip_id, user_id)
    ) engine=InnoDB;

create table trip_routes (
        is_primary bit not null,
        route_id varchar(64) not null,
        trip_id varchar(64) not null,
        primary key (route_id, trip_id)
    ) engine=InnoDB;

create table trips (
        actual_cost decimal(10,2),
        budget decimal(10,2),
        privacy_setting integer not null,
        status integer not null,
        created_at datetime(6) not null,
        end_date datetime(6),
        start_date datetime(6),
        updated_at datetime(6) not null,
        id varchar(64) not null,
        organizer_id varchar(64) not null,
        primary_route_id varchar(64),
        name varchar(200) not null,
        cover_url varchar(500),
        description TEXT,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table user_completed_routes (
        completed_at datetime(6) not null,
        route_id varchar(64) not null,
        user_id varchar(64) not null,
        primary key (route_id, user_id)
    ) engine=InnoDB;

create table user_equipment_items (
        quantity integer not null,
        equipment_item_id varchar(64) not null,
        user_id varchar(64) not null,
        notes TEXT,
        primary key (equipment_item_id, user_id)
    ) engine=InnoDB;

create table user_favorite_routes (
        created_at datetime(6) not null,
        id varchar(64) not null,
        route_id varchar(64) not null,
        user_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table user_route_completions (
        duration_minutes integer,
        completed_at datetime(6) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        route_id varchar(64) not null,
        user_id varchar(64) not null,
        notes TEXT,
        primary key (id)
    ) engine=InnoDB;

create table user_route_favorites (
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        route_id varchar(64) not null,
        user_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table users (
        status INT DEFAULT 0 not null,
        created_at datetime(6) not null,
        last_login_at datetime(6),
        updated_at datetime(6) not null,
        phone varchar(20),
        nickname varchar(50) not null,
        username varchar(50) not null,
        id varchar(64) not null,
        email varchar(100) not null,
        avatar_url varchar(500),
        bio TEXT,
        password varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create table water_plan_tags (
        tag varchar(50) not null,
        id varchar(64) not null,
        water_plan_id varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

create table water_plans (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        trip_id varchar(64),
        name varchar(200) not null,
        description TEXT,
        primary key (id)
    ) engine=InnoDB;

create table water_sources (
        elevation float(53),
        latitude float(53),
        longitude float(53),
        reliability float(53),
        requires_treatment bit not null,
        water_quality integer not null,
        water_type integer not null,
        created_at datetime(6) not null,
        last_verified datetime(6),
        updated_at datetime(6) not null,
        created_by varchar(64),
        id varchar(64) not null,
        verified_by_id varchar(64),
        description TEXT,
        name varchar(255) not null,
        notes TEXT,
        route_id varchar(255),
        primary key (id)
    ) engine=InnoDB;

create table waypoints (
        elevation float(53),
        latitude float(53) not null,
        longitude float(53) not null,
        sequence_number integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        type varchar(50),
        id varchar(64) not null,
        route_id varchar(64) not null,
        name varchar(200) not null,
        icon_url varchar(500),
        image_url varchar(500),
        description TEXT,
        primary key (id)
    ) engine=InnoDB;


-- ============ 索引 ============
create index idx_campsites_route_id on campsites (route_id);
create index idx_campsites_type on campsites (campsite_type);
create index idx_campsites_created_by on campsites (created_by);
create index idx_contacts_route_id on contacts (route_id);
create index idx_contacts_location on contacts (location);
create index idx_contacts_is_verified on contacts (is_verified);
create index idx_contacts_price on contacts (price);
create index idx_contacts_created_by on contacts (created_by);
create index idx_contacts_verified_by on contacts (verified_by);
create index idx_daily_plan_segments_plan_id on daily_plan_segments (daily_plan_id);
create index idx_daily_plan_segments_segment_id on daily_plan_segments (segment_id);
create index idx_daily_plans_route_id on daily_plans (route_id);
create index idx_daily_plans_day_number on daily_plans (day_number);
create index idx_equipment_items_category on equipment_items (category);
create index idx_equipment_items_created_by on equipment_items (created_by);
create index idx_equipment_list_items_list_id on equipment_list_items (equipment_list_id);
create index idx_equipment_list_items_item_id on equipment_list_items (equipment_item_id);
create index idx_equipment_lists_trip_id on equipment_lists (trip_id);
create index idx_equipment_lists_creator_id on equipment_lists (creator_id);
create index idx_equipment_lists_type on equipment_lists (type);
create index idx_guides_author_id on guides (author_id);
create index idx_guides_status on guides (status);
create index idx_guides_created_at on guides (created_at);
create index idx_guides_view_count on guides (view_count);
create index idx_guides_like_count on guides (like_count);
create index idx_hitchhike_contacts_route_id on hitchhike_contacts (route_id);
create index idx_hitchhike_contacts_location on hitchhike_contacts (location);
create index idx_hitchhike_contacts_created_by on hitchhike_contacts (created_by);
create index idx_marker_points_route_id on marker_points (route_id);
create index idx_marker_points_type on marker_points (marker_type);
create index idx_meal_days_meal_plan_id on meal_days (meal_plan_id);
create index idx_meal_days_day_number on meal_days (day_number);
create index idx_meal_food_items_meal_day_id on meal_food_items (meal_day_id);
create index idx_meal_food_items_food_item_id on meal_food_items (food_item_id);
create index idx_meal_food_items_meal_type on meal_food_items (meal_type);
create index idx_meal_items_meal_day_id on meal_items (meal_day_id);
create index idx_meal_items_meal_type on meal_items (meal_type);
create index idx_meal_plan_tags_plan_id on meal_plan_tags (meal_plan_id);
create index idx_meal_plan_tags_tag on meal_plan_tags (tag);
create index idx_meal_plans_trip_id on meal_plans (trip_id);
create index idx_meal_plans_created_by on meal_plans (created_by);
create index idx_path_points_segment_id on path_points (segment_id);
create index idx_path_points_sequence on path_points (sequence_number);
create index idx_path_points_type on path_points (point_type);
create index idx_poi_points_route_id on poi_points (route_id);
create index idx_poi_points_category on poi_points (category);
create index idx_poi_points_route_category on poi_points (route_id, category);
create index idx_route_contacts_route_id on route_contacts (route_id);
create index idx_route_contacts_contact_id on route_contacts (contact_id);
create index idx_route_contacts_contact_type on route_contacts (contact_type);
create index idx_route_images_route_id on route_images (route_id);
create index idx_route_images_is_cover on route_images (is_cover);
create index idx_route_images_sequence on route_images (sequence_number);
create index idx_route_ratings_overall on route_ratings (overall);
create index idx_route_tags_route_id on route_tags (route_id);
create index idx_route_tags_tag on route_tags (tag);
create index idx_routes_created_by on routes (created_by);
create index idx_routes_status on routes (status);
create index idx_routes_region on routes (region);
create index idx_routes_difficulty on routes (difficulty);
create index idx_routes_created_at on routes (created_at);
create index idx_seasonal_weather_route_weather_id on seasonal_weather (route_weather_id);
create index idx_seasonal_weather_season on seasonal_weather (season);
create index idx_segment_closures_segment_id on segment_closures (segment_id);
create index idx_segment_closures_dates on segment_closures (start_date, end_date);
create index idx_segment_closures_type on segment_closures (closure_type);
create index idx_segment_hazards_segment_id on segment_hazards (segment_id);
create index idx_segment_hazards_hazard on segment_hazards (hazard);
create index idx_segment_hazards_severity on segment_hazards (severity_level);
create index idx_segment_schemes_route_id on segment_schemes (route_id);
create index idx_segment_schemes_type on segment_schemes (scheme_type);
create index idx_segments_route_id on segments (route_id);
create index idx_segments_start_point on segments (start_point_id);
create index idx_segments_end_point on segments (end_point_id);
create index idx_supplies_route_id on supplies (route_id);
create index idx_supplies_supply_type on supplies (supply_type);
create index idx_supplies_elevation on supplies (elevation);
create index idx_supplies_last_verified on supplies (last_verified);
create index idx_supplies_created_by on supplies (created_by);
create index idx_trip_contacts_trip_id on trip_contacts (trip_id);
create index idx_trip_contacts_contact_id on trip_contacts (contact_id);
create index idx_trip_contacts_contact_type on trip_contacts (contact_type);
create index idx_trip_images_trip_id on trip_images (trip_id);
create index idx_trip_images_is_cover on trip_images (is_cover);
create index idx_trip_images_sequence on trip_images (sequence_number);
create index idx_trip_itinerary_trip_id on trip_itinerary (trip_id);
create index idx_trip_itinerary_day_number on trip_itinerary (day_number);
create index idx_trip_participants_trip_id on trip_participants (trip_id);
create index idx_trip_participants_user_id on trip_participants (user_id);
create index idx_trip_routes_trip_id on trip_routes (trip_id);
create index idx_trip_routes_route_id on trip_routes (route_id);
create index idx_trips_organizer_id on trips (organizer_id);
create index idx_trips_status on trips (status);
create index idx_trips_start_date on trips (start_date);
create index idx_trips_end_date on trips (end_date);
create index idx_trips_date_range on trips (start_date, end_date);
create index idx_trips_privacy on trips (privacy_setting);
create index idx_user_completed_routes_user_id on user_completed_routes (user_id);
create index idx_user_completed_routes_route_id on user_completed_routes (route_id);
create index idx_user_completed_routes_completed_at on user_completed_routes (completed_at);
create index idx_user_equipment_items_user_id on user_equipment_items (user_id);
create index idx_user_equipment_items_item_id on user_equipment_items (equipment_item_id);
create index idx_user_favorite_routes_user_id on user_favorite_routes (user_id);
create index idx_user_favorite_routes_route_id on user_favorite_routes (route_id);
create index idx_user_route_completions_user_id on user_route_completions (user_id);
create index idx_user_route_completions_route_id on user_route_completions (route_id);
create index idx_user_route_completions_completed_at on user_route_completions (completed_at);
create index idx_user_route_favorites_user_id on user_route_favorites (user_id);
create index idx_user_route_favorites_route_id on user_route_favorites (route_id);
create index idx_user_route_favorites_created_at on user_route_favorites (created_at);
create index idx_water_plan_tags_plan_id on water_plan_tags (water_plan_id);
create index idx_water_plan_tags_tag on water_plan_tags (tag);
create index idx_water_plans_trip_id on water_plans (trip_id);
create index idx_water_plans_created_by on water_plans (created_by);
create index idx_waypoints_route_id on waypoints (route_id);
create index idx_waypoints_sequence on waypoints (sequence_number);


-- ============ 约束与外键 ============
alter table route_facilities add constraint idx_route_facilities_route_id unique (route_id);
alter table route_ratings add constraint idx_route_ratings_route_id unique (route_id);
alter table route_tags add constraint UKthonw6fmo6wbp72fkdc84tamx unique (route_id, tag);
alter table route_weather add constraint idx_route_weather_route_id unique (route_id);
alter table user_favorite_routes add constraint UKdi22hs7q68ugsjbbp952iorqh unique (user_id, route_id);
alter table user_route_favorites add constraint uk_user_route_favorite unique (user_id, route_id);
alter table users add constraint idx_users_email unique (email);
alter table users add constraint idx_users_username unique (username);
alter table segment_keypoints add constraint FKm9o4ytf284blfd119sotdjujh foreign key (segment_id) references segments (id);
alter table segment_keypoints add constraint FKiv0aw0vfqqu30037lhmhegt2n foreign key (waypoint_id) references waypoints (id);
alter table template_equipment_items add constraint FK651vvufe3ppootjw5t6ddnaym foreign key (template_id) references equipment_templates (id);
alter table template_seasons add constraint FKm5yk60quwg58w2jtnlp7ui4dl foreign key (template_id) references equipment_templates (id);
alter table template_tags add constraint FKt9d0jw8dfecncrn0yk7m69m67 foreign key (template_id) references equipment_templates (id);
alter table user_completed_routes add constraint fk_user_completed_routes_route_id foreign key (route_id) references routes (id);
alter table user_completed_routes add constraint fk_user_completed_routes_user_id foreign key (user_id) references users (id);
alter table user_equipment_items add constraint fk_user_equipment_items_equipment_item_id foreign key (equipment_item_id) references equipment_items (id);
alter table user_equipment_items add constraint fk_user_equipment_items_user_id foreign key (user_id) references users (id);
alter table user_favorite_routes add constraint fk_user_favorite_routes_route_id foreign key (route_id) references routes (id);
alter table user_favorite_routes add constraint fk_user_favorite_routes_user_id foreign key (user_id) references users (id);
alter table water_sources add constraint FKojh9gwabn487wjwv9lkb06cyn foreign key (created_by) references users (id);
alter table water_sources add constraint FKpnm3cq5r4l5kdowq1xlqgdym7 foreign key (verified_by_id) references users (id);
