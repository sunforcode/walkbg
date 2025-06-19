#!/bin/bash

# 路线JSON数据验证脚本
# 使用方法: ./validate_route.sh route_data.json

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }

# 显示帮助信息
show_help() {
    echo "WalkBG 路线JSON数据验证工具"
    echo ""
    echo "用法: $0 route_data.json"
    echo ""
    echo "功能:"
    echo "  - 验证JSON格式"
    echo "  - 检查所有字段的key和value类型"
    echo "  - 验证枚举值范围"
    echo ""
    echo "选项:"
    echo "  -h, --help    显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 my_route.json"
}

# 验证字符串类型
validate_string() {
    local value="$1"
    local field_name="$2"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    # 检查是否为字符串类型
    if ! echo "$value" | jq -e 'type == "string"' >/dev/null 2>&1; then
        print_error "$field_name 必须是字符串类型，当前类型: $(echo "$value" | jq -r 'type')"
        return 1
    fi
    return 0
}

# 验证数字类型
validate_number() {
    local value="$1"
    local field_name="$2"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    # 检查是否为数字类型
    if ! echo "$value" | jq -e 'type == "number"' >/dev/null 2>&1; then
        print_error "$field_name 必须是数字类型，当前类型: $(echo "$value" | jq -r 'type')"
        return 1
    fi
    return 0
}

# 验证整数类型
validate_integer() {
    local value="$1"
    local field_name="$2"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    # 检查是否为整数
    local num_value=$(echo "$value" | jq -r '.')
    if ! [[ "$num_value" =~ ^-?[0-9]+$ ]]; then
        print_error "$field_name 必须是整数，当前值: $num_value"
        return 1
    fi
    return 0
}

# 验证布尔类型
validate_boolean() {
    local value="$1"
    local field_name="$2"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    # 检查是否为布尔类型
    if ! echo "$value" | jq -e 'type == "boolean"' >/dev/null 2>&1; then
        print_error "$field_name 必须是布尔类型，当前类型: $(echo "$value" | jq -r 'type')"
        return 1
    fi
    return 0
}

# 验证数组类型
validate_array() {
    local value="$1"
    local field_name="$2"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    # 检查是否为数组类型
    if ! echo "$value" | jq -e 'type == "array"' >/dev/null 2>&1; then
        print_error "$field_name 必须是数组类型，当前类型: $(echo "$value" | jq -r 'type')"
        return 1
    fi
    return 0
}

# 验证枚举值
validate_enum() {
    local value="$1"
    local field_name="$2"
    local min_val="$3"
    local max_val="$4"

    if [[ "$value" == "null" ]]; then
        return 0  # null值允许
    fi

    local num_value=$(echo "$value" | jq -r '.')
    if [[ "$num_value" -lt "$min_val" ]] || [[ "$num_value" -gt "$max_val" ]]; then
        print_error "$field_name 必须在 $min_val-$max_val 范围内，当前值: $num_value"
        return 1
    fi
    return 0
}

# 验证Route主字段
validate_route_fields() {
    local json_file="$1"
    local errors=0

    print_info "验证Route主字段..."

    # 必需字段验证
    local name_val=$(jq -r '.name // null' "$json_file")
    if [[ "$name_val" == "null" ]] || [[ "$name_val" == "" ]]; then
        print_error "name 字段是必需的且不能为空"
        ((errors++))
    else
        validate_string "\"$name_val\"" "name" || ((errors++))
    fi

    local created_by_val=$(jq -r '.created_by // null' "$json_file")
    if [[ "$created_by_val" == "null" ]] || [[ "$created_by_val" == "" ]]; then
        print_error "created_by 字段是必需的且不能为空"
        ((errors++))
    else
        validate_string "\"$created_by_val\"" "created_by" || ((errors++))
    fi

    # 可选字符串字段
    local string_fields=("id" "description" "region" "region_id" "cover_url" "map_data_id" "default_map_id")
    for field in "${string_fields[@]}"; do
        if jq -e ".$field" "$json_file" >/dev/null 2>&1; then
            local val=$(jq ".$field" "$json_file")
            validate_string "$val" "$field" || ((errors++))
        fi
    done

    # 数字字段
    local number_fields=("distance" "latitude" "longitude" "altitude" "elevation_gain" "elevation_loss")
    for field in "${number_fields[@]}"; do
        if jq -e ".$field" "$json_file" >/dev/null 2>&1; then
            local val=$(jq ".$field" "$json_file")
            validate_number "$val" "$field" || ((errors++))
        fi
    done

    # 整数字段
    local integer_fields=("duration")
    for field in "${integer_fields[@]}"; do
        if jq -e ".$field" "$json_file" >/dev/null 2>&1; then
            local val=$(jq ".$field" "$json_file")
            validate_integer "$val" "$field" || ((errors++))
        fi
    done

    # 枚举字段验证
    if jq -e '.difficulty' "$json_file" >/dev/null 2>&1; then
        local val=$(jq '.difficulty' "$json_file")
        validate_integer "$val" "difficulty" || ((errors++))
        if [[ "$val" != "null" ]]; then
            validate_enum "$val" "difficulty" 1 5 || ((errors++))
        fi
    fi

    if jq -e '.route_type' "$json_file" >/dev/null 2>&1; then
        local val=$(jq '.route_type' "$json_file")
        validate_integer "$val" "route_type" || ((errors++))
        if [[ "$val" != "null" ]]; then
            validate_enum "$val" "route_type" 0 3 || ((errors++))
        fi
    fi

    if jq -e '.route_direction' "$json_file" >/dev/null 2>&1; then
        local val=$(jq '.route_direction' "$json_file")
        validate_integer "$val" "route_direction" || ((errors++))
        if [[ "$val" != "null" ]]; then
            validate_enum "$val" "route_direction" 0 360 || ((errors++))
        fi
    fi

    if jq -e '.status' "$json_file" >/dev/null 2>&1; then
        local val=$(jq '.status' "$json_file")
        validate_integer "$val" "status" || ((errors++))
        if [[ "$val" != "null" ]]; then
            validate_enum "$val" "status" 0 2 || ((errors++))
        fi
    fi

    # 数组字段
    local array_fields=("tags" "seasons")
    for field in "${array_fields[@]}"; do
        if jq -e ".$field" "$json_file" >/dev/null 2>&1; then
            local val=$(jq ".$field" "$json_file")
            validate_array "$val" "$field" || ((errors++))
        fi
    done

    return $errors
}

# 验证Waypoints
validate_waypoints() {
    local json_file="$1"
    local errors=0

    if ! jq -e '.waypoints' "$json_file" >/dev/null 2>&1; then
        return 0
    fi

    print_info "验证waypoints..."

    local waypoints=$(jq '.waypoints' "$json_file")
    validate_array "$waypoints" "waypoints" || ((errors++))

    local count=$(jq '.waypoints | length' "$json_file")
    for ((i=0; i<count; i++)); do
        local wp=$(jq ".waypoints[$i]" "$json_file")

        # 必需字段
        local name_val=$(echo "$wp" | jq -r '.name // null')
        if [[ "$name_val" == "null" ]]; then
            print_error "waypoints[$i].name 是必需字段"
            ((errors++))
        fi

        # 数字字段
        local lat=$(echo "$wp" | jq '.latitude // null')
        local lng=$(echo "$wp" | jq '.longitude // null')
        local elev=$(echo "$wp" | jq '.elevation // null')

        if [[ "$lat" == "null" ]]; then
            print_error "waypoints[$i].latitude 是必需字段"
            ((errors++))
        else
            validate_number "$lat" "waypoints[$i].latitude" || ((errors++))
        fi

        if [[ "$lng" == "null" ]]; then
            print_error "waypoints[$i].longitude 是必需字段"
            ((errors++))
        else
            validate_number "$lng" "waypoints[$i].longitude" || ((errors++))
        fi

        if [[ "$elev" != "null" ]]; then
            validate_number "$elev" "waypoints[$i].elevation" || ((errors++))
        fi

        # 整数字段
        local seq_num=$(echo "$wp" | jq '.sequence_number // null')
        if [[ "$seq_num" == "null" ]]; then
            print_error "waypoints[$i].sequence_number 是必需字段"
            ((errors++))
        else
            validate_integer "$seq_num" "waypoints[$i].sequence_number" || ((errors++))
            local seq_val=$(echo "$seq_num" | jq -r '.')
            if [[ "$seq_val" -le 0 ]]; then
                print_error "waypoints[$i].sequence_number 必须大于0，当前值: $seq_val"
                ((errors++))
            fi
        fi

        # 字符串字段
        local string_fields=("description" "type" "icon_url" "image_url")
        for field in "${string_fields[@]}"; do
            if echo "$wp" | jq -e ".$field" >/dev/null 2>&1; then
                local val=$(echo "$wp" | jq ".$field")
                validate_string "$val" "waypoints[$i].$field" || ((errors++))
            fi
        done
    done

    return $errors
}

# 验证Segments
validate_segments() {
    local json_file="$1"
    local errors=0

    if ! jq -e '.segments' "$json_file" >/dev/null 2>&1; then
        return 0
    fi

    print_info "验证segments..."

    local segments=$(jq '.segments' "$json_file")
    validate_array "$segments" "segments" || ((errors++))

    local count=$(jq '.segments | length' "$json_file")
    for ((i=0; i<count; i++)); do
        local seg=$(jq ".segments[$i]" "$json_file")

        # 数字字段
        local number_fields=("distance" "elevation_gain" "elevation_loss" "estimated_time")
        for field in "${number_fields[@]}"; do
            if echo "$seg" | jq -e ".$field" >/dev/null 2>&1; then
                local val=$(echo "$seg" | jq ".$field")
                validate_number "$val" "segments[$i].$field" || ((errors++))
            fi
        done

        # 整数字段
        local integer_fields=("difficulty" "route_type" "traffic_level")
        for field in "${integer_fields[@]}"; do
            if echo "$seg" | jq -e ".$field" >/dev/null 2>&1; then
                local val=$(echo "$seg" | jq ".$field")
                validate_integer "$val" "segments[$i].$field" || ((errors++))

                # 枚举验证
                if [[ "$field" == "difficulty" ]] && [[ "$val" != "null" ]]; then
                    validate_enum "$val" "segments[$i].$field" 1 5 || ((errors++))
                elif [[ "$field" == "route_type" ]] && [[ "$val" != "null" ]]; then
                    validate_enum "$val" "segments[$i].$field" 0 3 || ((errors++))
                elif [[ "$field" == "traffic_level" ]] && [[ "$val" != "null" ]]; then
                    validate_enum "$val" "segments[$i].$field" 0 5 || ((errors++))
                fi
            fi
        done

        # 字符串字段
        local string_fields=("name" "description" "terrain" "surface_type" "notes")
        for field in "${string_fields[@]}"; do
            if echo "$seg" | jq -e ".$field" >/dev/null 2>&1; then
                local val=$(echo "$seg" | jq ".$field")
                validate_string "$val" "segments[$i].$field" || ((errors++))
            fi
        done
    done

    return $errors
}

# 验证其他关联对象
validate_associated_objects() {
    local json_file="$1"
    local errors=0

    # 验证images
    if jq -e '.images' "$json_file" >/dev/null 2>&1; then
        print_info "验证images..."
        local images=$(jq '.images' "$json_file")
        validate_array "$images" "images" || ((errors++))

        local count=$(jq '.images | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local img=$(jq ".images[$i]" "$json_file")

            # 字符串字段
            if echo "$img" | jq -e '.imageUrl' >/dev/null 2>&1; then
                local val=$(echo "$img" | jq '.imageUrl')
                validate_string "$val" "images[$i].imageUrl" || ((errors++))
            fi

            # 布尔字段
            if echo "$img" | jq -e '.isCover' >/dev/null 2>&1; then
                local val=$(echo "$img" | jq '.isCover')
                validate_boolean "$val" "images[$i].isCover" || ((errors++))
            fi

            # 整数字段
            if echo "$img" | jq -e '.sequenceNumber' >/dev/null 2>&1; then
                local val=$(echo "$img" | jq '.sequenceNumber')
                validate_integer "$val" "images[$i].sequenceNumber" || ((errors++))
            fi
        done
    fi

    # 验证markerPoints
    if jq -e '.markerPoints' "$json_file" >/dev/null 2>&1; then
        print_info "验证markerPoints..."
        local markers=$(jq '.markerPoints' "$json_file")
        validate_array "$markers" "markerPoints" || ((errors++))

        local count=$(jq '.markerPoints | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local marker=$(jq ".markerPoints[$i]" "$json_file")

            # 字符串字段
            local string_fields=("name" "description" "color" "icon_url")
            for field in "${string_fields[@]}"; do
                if echo "$marker" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$marker" | jq ".$field")
                    validate_string "$val" "markerPoints[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            local number_fields=("latitude" "longitude" "elevation")
            for field in "${number_fields[@]}"; do
                if echo "$marker" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$marker" | jq ".$field")
                    validate_number "$val" "markerPoints[$i].$field" || ((errors++))
                fi
            done

            # 整数字段
            local integer_fields=("marker_type" "sequenceNumber")
            for field in "${integer_fields[@]}"; do
                if echo "$marker" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$marker" | jq ".$field")
                    validate_integer "$val" "markerPoints[$i].$field" || ((errors++))
                fi
            done
        done
    fi

    # 验证dailyPlans
    if jq -e '.dailyPlans' "$json_file" >/dev/null 2>&1; then
        print_info "验证dailyPlans..."
        local plans=$(jq '.dailyPlans' "$json_file")
        validate_array "$plans" "dailyPlans" || ((errors++))

        local count=$(jq '.dailyPlans | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local plan=$(jq ".dailyPlans[$i]" "$json_file")

            # 字符串字段
            local string_fields=("title" "description" "notes")
            for field in "${string_fields[@]}"; do
                if echo "$plan" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$plan" | jq ".$field")
                    validate_string "$val" "dailyPlans[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            local number_fields=("distance" "elevation_gain" "elevation_loss" "estimated_time")
            for field in "${number_fields[@]}"; do
                if echo "$plan" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$plan" | jq ".$field")
                    validate_number "$val" "dailyPlans[$i].$field" || ((errors++))
                fi
            done

            # 整数字段
            if echo "$plan" | jq -e '.dayNumber' >/dev/null 2>&1; then
                local val=$(echo "$plan" | jq '.dayNumber')
                validate_integer "$val" "dailyPlans[$i].dayNumber" || ((errors++))
                local day_val=$(echo "$val" | jq -r '.')
                if [[ "$day_val" -le 0 ]]; then
                    print_error "dailyPlans[$i].dayNumber 必须大于0，当前值: $day_val"
                    ((errors++))
                fi
            fi
        done
    fi

    # 验证waterSources
    if jq -e '.waterSources' "$json_file" >/dev/null 2>&1; then
        print_info "验证waterSources..."
        local sources=$(jq '.waterSources' "$json_file")
        validate_array "$sources" "waterSources" || ((errors++))

        local count=$(jq '.waterSources | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local source=$(jq ".waterSources[$i]" "$json_file")

            # 必需字段验证
            local name_val=$(echo "$source" | jq -r '.name // null')
            if [[ "$name_val" == "null" ]] || [[ "$name_val" == "" ]]; then
                print_error "waterSources[$i].name 是必需字段且不能为空"
                ((errors++))
            else
                validate_string "\"$name_val\"" "waterSources[$i].name" || ((errors++))
            fi

            # 可选字符串字段
            local string_fields=("id" "description" "notes")
            for field in "${string_fields[@]}"; do
                if echo "$source" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$source" | jq ".$field")
                    validate_string "$val" "waterSources[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            local number_fields=("latitude" "longitude" "elevation" "reliability")
            for field in "${number_fields[@]}"; do
                if echo "$source" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$source" | jq ".$field")
                    validate_number "$val" "waterSources[$i].$field" || ((errors++))
                fi
            done

            # 整数枚举字段
            if echo "$source" | jq -e '.water_type' >/dev/null 2>&1; then
                local val=$(echo "$source" | jq '.water_type')
                validate_integer "$val" "waterSources[$i].water_type" || ((errors++))
                if [[ "$val" != "null" ]]; then
                    # 水源类型: 0=天然水源, 1=处理过的水源, 2=瓶装水, 3=其他
                    validate_enum "$val" "waterSources[$i].water_type" 0 3 || ((errors++))
                fi
            fi

            if echo "$source" | jq -e '.water_quality' >/dev/null 2>&1; then
                local val=$(echo "$source" | jq '.water_quality')
                validate_integer "$val" "waterSources[$i].water_quality" || ((errors++))
                if [[ "$val" != "null" ]]; then
                    # 水质等级: 0=优质, 1=良好, 2=一般, 3=较差, 4=未知
                    validate_enum "$val" "waterSources[$i].water_quality" 0 4 || ((errors++))
                fi
            fi

            # 布尔字段
            if echo "$source" | jq -e '.requires_treatment' >/dev/null 2>&1; then
                local val=$(echo "$source" | jq '.requires_treatment')
                validate_boolean "$val" "waterSources[$i].requires_treatment" || ((errors++))
            fi

            # 时间戳字段验证
            local timestamp_fields=("created_at" "updated_at" "last_verified")
            for field in "${timestamp_fields[@]}"; do
                if echo "$source" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$source" | jq ".$field")
                    # 时间戳可以是数字（Unix时间戳）或字符串（ISO格式）
                    if [[ "$val" != "null" ]]; then
                        local val_type=$(echo "$val" | jq -r 'type')
                        if [[ "$val_type" != "number" ]] && [[ "$val_type" != "string" ]]; then
                            print_error "waterSources[$i].$field 必须是数字（时间戳）或字符串（ISO格式），当前类型: $val_type"
                            ((errors++))
                        fi
                    fi
                fi
            done

            # 关联对象ID字段
            local id_fields=("route_id" "verified_by_id" "created_by")
            for field in "${id_fields[@]}"; do
                if echo "$source" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$source" | jq ".$field")
                    validate_string "$val" "waterSources[$i].$field" || ((errors++))
                fi
            done
        done
    fi

    # 验证supplies
    if jq -e '.supplies' "$json_file" >/dev/null 2>&1; then
        print_info "验证supplies..."
        local supplies=$(jq '.supplies' "$json_file")
        validate_array "$supplies" "supplies" || ((errors++))

        local count=$(jq '.supplies | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local supply=$(jq ".supplies[$i]" "$json_file")

            # 字符串字段
            local string_fields=("name" "description" "last_verified" "updated_by")
            for field in "${string_fields[@]}"; do
                if echo "$supply" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$supply" | jq ".$field")
                    validate_string "$val" "supplies[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            local number_fields=("latitude" "longitude" "elevation" "price")
            for field in "${number_fields[@]}"; do
                if echo "$supply" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$supply" | jq ".$field")
                    validate_number "$val" "supplies[$i].$field" || ((errors++))
                fi
            done

            # 整数枚举字段
            if echo "$supply" | jq -e '.supply_type' >/dev/null 2>&1; then
                local val=$(echo "$supply" | jq '.supply_type')
                validate_integer "$val" "supplies[$i].supply_type" || ((errors++))
            fi
        done
    fi

    # 验证campsites
    if jq -e '.campsites' "$json_file" >/dev/null 2>&1; then
        print_info "验证campsites..."
        local campsites=$(jq '.campsites' "$json_file")
        validate_array "$campsites" "campsites" || ((errors++))

        local count=$(jq '.campsites | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local campsite=$(jq ".campsites[$i]" "$json_file")

            # 字符串字段
            local string_fields=("name" "description" "notes")
            for field in "${string_fields[@]}"; do
                if echo "$campsite" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$campsite" | jq ".$field")
                    validate_string "$val" "campsites[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            local number_fields=("latitude" "longitude" "elevation")
            for field in "${number_fields[@]}"; do
                if echo "$campsite" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$campsite" | jq ".$field")
                    validate_number "$val" "campsites[$i].$field" || ((errors++))
                fi
            done

            # 整数字段
            local integer_fields=("campsite_type" "capacity")
            for field in "${integer_fields[@]}"; do
                if echo "$campsite" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$campsite" | jq ".$field")
                    validate_integer "$val" "campsites[$i].$field" || ((errors++))
                fi
            done
        done
    fi

    # 验证hitchhikeContacts
    if jq -e '.hitchhikeContacts' "$json_file" >/dev/null 2>&1; then
        print_info "验证hitchhikeContacts..."
        local contacts=$(jq '.hitchhikeContacts' "$json_file")
        validate_array "$contacts" "hitchhikeContacts" || ((errors++))

        local count=$(jq '.hitchhikeContacts | length' "$json_file")
        for ((i=0; i<count; i++)); do
            local contact=$(jq ".hitchhikeContacts[$i]" "$json_file")

            # 字符串字段
            local string_fields=("name" "description" "phone" "location")
            for field in "${string_fields[@]}"; do
                if echo "$contact" | jq -e ".$field" >/dev/null 2>&1; then
                    local val=$(echo "$contact" | jq ".$field")
                    validate_string "$val" "hitchhikeContacts[$i].$field" || ((errors++))
                fi
            done

            # 数字字段
            if echo "$contact" | jq -e '.price' >/dev/null 2>&1; then
                local val=$(echo "$contact" | jq '.price')
                validate_number "$val" "hitchhikeContacts[$i].price" || ((errors++))
            fi

            # 布尔字段
            if echo "$contact" | jq -e '.verified' >/dev/null 2>&1; then
                local val=$(echo "$contact" | jq '.verified')
                validate_boolean "$val" "hitchhikeContacts[$i].verified" || ((errors++))
            fi
        done
    fi

    return $errors
}

# 主验证函数
validate_route_json() {
    local json_file="$1"
    local total_errors=0

    print_info "开始验证路线JSON: $json_file"
    echo ""

    # 验证JSON格式
    if ! jq . "$json_file" >/dev/null 2>&1; then
        print_error "无效的JSON格式"
        return 1
    fi
    print_success "JSON格式正确"

    # 执行各项验证
    validate_route_fields "$json_file"
    total_errors=$((total_errors + $?))

    validate_waypoints "$json_file"
    total_errors=$((total_errors + $?))

    validate_segments "$json_file"
    total_errors=$((total_errors + $?))

    validate_associated_objects "$json_file"
    total_errors=$((total_errors + $?))

    echo ""
    if [ $total_errors -eq 0 ]; then
        print_success "✅ 验证通过！JSON数据格式正确，可以用于创建路线"
        return 0
    else
        print_error "❌ 验证失败！发现 $total_errors 个错误，请修正后重试"
        return 1
    fi
}

# 解析命令行参数
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# 检查参数
if [ -z "$1" ]; then
    print_error "错误: 必须提供JSON文件路径"
    show_help
    exit 1
fi

JSON_FILE="$1"

# 检查文件是否存在
if [ ! -f "$JSON_FILE" ]; then
    print_error "错误: 文件不存在: $JSON_FILE"
    exit 1
fi

# 检查依赖
if ! command -v jq &> /dev/null; then
    print_error "错误: 需要安装jq工具"
    print_info "安装命令: brew install jq"
    exit 1
fi

# 执行验证
validate_route_json "$JSON_FILE"
