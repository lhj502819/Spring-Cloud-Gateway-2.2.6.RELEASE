local tokens_key = KEYS[1] --request_rate_limiter.{'id'}.tokens
local timestamp_key = KEYS[2] --request_rate_limiter.{'id'}.timestamp
--redis.log(redis.LOG_WARNING, "tokens_key " .. tokens_key)

local rate = tonumber(ARGV[1]) -- 允许用户每秒执行的请求数（填充令牌的速度） 20
local capacity = tonumber(ARGV[2]) -- 令牌桶的容量 50
--第一次请求1618841535
--第二次请求1618841537
local now = tonumber(ARGV[3]) -- 当前时间戳
local requested = tonumber(ARGV[4]) -- 每个请求消耗的令牌数 2

local fill_time = capacity/rate --容量/填充令牌的速度 ---  填充的次数
local ttl = math.floor(fill_time*2) -- key的过期时间

--redis.log(redis.LOG_WARNING, "rate " .. ARGV[1])
--redis.log(redis.LOG_WARNING, "capacity " .. ARGV[2])
--redis.log(redis.LOG_WARNING, "now " .. ARGV[3])
--redis.log(redis.LOG_WARNING, "requested " .. ARGV[4])
--redis.log(redis.LOG_WARNING, "filltime " .. fill_time)
--redis.log(redis.LOG_WARNING, "ttl " .. ttl)

--第一次请求 last_tokens = 50
--第二次请求 last_tokens = 48
local last_tokens = tonumber(redis.call("get", tokens_key)) --获取最后一次请求后剩余的令牌数量
if last_tokens == nil then  --如果剩余的令牌为空，则初始化为容量
  last_tokens = capacity
end
--redis.log(redis.LOG_WARNING, "last_tokens " .. last_tokens)

--第一次请求 last_refreshed = 0
--第二次请求 last_refreshed = 1618841535
local last_refreshed = tonumber(redis.call("get", timestamp_key)) -- 获取最后一次请求的时间
if last_refreshed == nil then -- 如果最后一次请求的时间为空，则设置为0
  last_refreshed = 0
end
--redis.log(redis.LOG_WARNING, "last_refreshed " .. last_refreshed)

--第一次请求 delta=(0 , 1618841535 - 0 ) = 1618841535
--第二次请求 delta = (0 , 1618841537 - 1618841535) = 2
local delta = math.max(0, now-last_refreshed) --当前时间减去最后刷新时间 -----  两次请求的时间差
--第一次请求 filled_tokens = math.min(50 , 50 + (1618841535 * 20)) = 50 --- 总容量
--第二次请求 filled_tokens = math.min(50 , 48 + (2 * 20)) = 50，这里我们可以发现，当两次请求的间隔大于1s时，都会将filled_tokens重新设置为桶的最大容量
    --当两次请求的间隔小于1s时，math.min函数才会取","后边的值，当然，小于1s时，delta就是0了，因此filled_tokens就是上次剩余的令牌数量
local filled_tokens = math.min(capacity, last_tokens+(delta*rate)) -- 计算
-- 第一次请求 50 >= 2 允许
local allowed = filled_tokens >= requested --当filled_tokens >= 一次请求消耗的令牌数时，则允许

local new_tokens = filled_tokens
local allowed_num = 0
if allowed then
  --如果允许则扣除令牌
  new_tokens = filled_tokens - requested
  allowed_num = 1
end

--redis.log(redis.LOG_WARNING, "delta " .. delta)
--redis.log(redis.LOG_WARNING, "filled_tokens " .. filled_tokens)
--redis.log(redis.LOG_WARNING, "allowed_num " .. allowed_num)
--redis.log(redis.LOG_WARNING, "new_tokens " .. new_tokens)

if ttl > 0 then
  --设置剩余令牌数
  redis.call("setex", tokens_key, ttl, new_tokens)
  --设置本次请求时间戳
  redis.call("setex", timestamp_key, ttl, now)
end

-- return { allowed_num, new_tokens, capacity, filled_tokens, requested, new_tokens }
return { allowed_num, new_tokens }
