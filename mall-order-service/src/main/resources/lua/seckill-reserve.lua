-- KEYS[1]：秒杀商品详情缓存；ARGV[1]：服务本地的 ISO 当前时间。
-- 0：预扣成功；1：下单失败。
local raw = redis.call('GET', KEYS[1])
if not raw then
    return 1
end

local valid, snapshot = pcall(cjson.decode, raw)
if not valid or type(snapshot) ~= 'table' or type(snapshot.data) ~= 'table' then
    return 1
end

local product = snapshot.data
if product.isSeckill ~= true then
    return 1
end

-- 补齐秒和小数位，使 Java ISO 本地时间可以按字符串比较。
local function normalizeTime(value)
    if type(value) ~= 'string' then
        return nil
    end

    local base = string.sub(value, 1, 19)
    if #value == 16 then
        base = value .. ':00'
    end
    if not string.match(base, '^%d%d%d%d%-%d%d%-%d%dT%d%d:%d%d:%d%d$') then
        return nil
    end

    local fraction = string.match(value, '%.(%d+)') or ''
    return base .. '.' .. string.sub(fraction .. '000000000', 1, 9)
end

local now = normalizeTime(ARGV[1])
local startTime = normalizeTime(product.seckillStartTime)
local endTime = normalizeTime(product.seckillEndTime)

if not now or not startTime or not endTime or startTime >= endTime then
    return 1
end
if now < startTime or now >= endTime then
    return 1
end

local stock = product.stock
if type(stock) ~= 'number' or stock < 1 or stock ~= math.floor(stock) then
    return 1
end

-- 只修改库存，避免 cjson 重新编码把空数组变成对象。
local updated, count = string.gsub(raw, '("stock"%s*:%s*)%d+', function(prefix)
    return prefix .. string.format('%.0f', stock - 1)
end, 1)

if count ~= 1 then
    return 1
end

redis.call('SET', KEYS[1], updated, 'KEEPTTL')
return 0
