local function floorDiv(x, y)
    local r = x / y;
    if ((x ^ y) < 0 and (r * y ~= x)) then
        r = r - 1;
    end
    return math.floor(r);
end
local function floorMod(x, y)
    return x - floorDiv(x, y) * y;
end
local function getDayOfTime(time)
    return floorMod(time, 86400);
end
local function getHour(dateTime)
    return math.floor(dateTime / 3600);
end
local function getSecond(dateTime)
    return math.floor(dateTime % 3600);
end
local function getMinutes(dateTime)
    return math.floor(dateTime % 3600 / 60);
end
local function secondToMinutes(second)
    return second / 60;
end
local function getTime()
    local time = redis.call("GET", "TIME");
    if ("false" == tostring(time)) then
        --redis.replicate_commands();
        time = redis.call("TIME")[1];
        local expire = 3600 - getSecond(getDayOfTime(time));
        time = redis.call("SETEX", "TIME", expire, time);
    end
    return time;
end
local function getKey(sequenceKey, tenantKey, appKey, module)
    return sequenceKey .. "_" .. tenantKey .. "_" .. appKey .. ":" .. module;
end
local function getTime(timeZone, time)
    local result = {};
    --time zone
    timeZone = timeZone * 3600;
    --current time
    --redis.replicate_commands();
    --local time = redis.call("TIME")[1] + timeZone;
    --daysz
    local days = { 1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335 }
    --:year-benchmarkYear
    local benchmarkYear = 2000;
    local SECONDS_PER_DAY = 86400;
    local DAYS_PER_CYCLE = 146097;
    local DAYS_0000_TO_1970 = (730485 - 10957);
    local epochDay = floorDiv(time, SECONDS_PER_DAY);
    local dayOfTime = getDayOfTime(time);
    local hour = getHour(dayOfTime);
    local second = getSecond(dayOfTime);
    local zeroDay = epochDay + DAYS_0000_TO_1970;
    zeroDay = zeroDay - 60;
    local adjust = 0;
    if (zeroDay < 0) then
        --// adjust negative years to positive for calculation
        local adjustCycles = math.floor((zeroDay + 1) / DAYS_PER_CYCLE - 1);
        adjust = adjustCycles * 400;
        zeroDay = zeroDay + (-adjustCycles * DAYS_PER_CYCLE);
    end
    local yearEst = math.floor((400 * zeroDay + 591) / DAYS_PER_CYCLE);
    local doyEst = math.floor(zeroDay - (365 * yearEst + yearEst / 4 - (yearEst / 100) + yearEst / 400));
    if (doyEst < 0) then
        yearEst = yearEst - 1;
        doyEst = math.floor(zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400));
    end
    yearEst = yearEst + adjust;
    local marchDoy0 = doyEst;
    local marchMonth0 = math.floor((marchDoy0 * 5 + 2) / 153);
    local month = math.floor((marchMonth0 + 2) % 12 + 1);
    local dom = marchDoy0 - math.floor((marchMonth0 * 306 + 5) / 10) + 1;
    yearEst = yearEst + math.floor((marchMonth0 / 10));
    local day = days[month] + dom;
    if ((((yearEst % 4) == 0) and ((yearEst % 100) ~= 0 or (yearEst % 400) == 0)) and month > 2) then
        day = day + 1;
    end
    table.insert(result, time);
    table.insert(result, day);
    table.insert(result, yearEst);
    table.insert(result, month);
    table.insert(result, dom);
    table.insert(result, hour);
    local expire = 3600 - second;
    table.insert(result, expire);
    return result;
end
local function getSequenceTime(timeZone, time)
    local SEQUENCE_TIME = "DEFAULT_SEQUENCE_TIME";
    local result = redis.call("GET", SEQUENCE_TIME);
    if ("false" == tostring(result)) then
        local SEQUENCE_TIME_FORMAT = "%d%03d%02d";
        local time = getTime(timeZone, time);
        result = string.format(SEQUENCE_TIME_FORMAT, time[3], time[2], time[6]);
        redis.call("SETEX", SEQUENCE_TIME, time[7], result);
    end
    return result;
end
local function getSequenceTimeExpire()
    return redis.call("TTL", "DEFAULT_SEQUENCE_TIME");
end
local function next(sequenceKey, tenantKey, appKey, module, quantity, timeZone, time)
    local sequenceResult;
    local sequenceTime = getSequenceTime(timeZone, time);
    local key = sequenceTime .. getKey(sequenceKey, tenantKey, appKey, module);
    local result = {};
    if (tonumber(quantity) > 1) then
        sequenceResult = redis.call("INCRBY", key, quantity);
        for i = sequenceResult - tonumber(quantity) + 1, sequenceResult, 1 do
            table.insert(result, sequenceTime .. string.format("%010d", i))
        end
    else
        sequenceResult = redis.call("INCR", key)
        table.insert(result, sequenceTime .. string.format("%010d", sequenceResult));
    end
    if (sequenceResult == quantity) then
        local randomSec = math.random(10, 60);
        redis.call("EXPIRE", key, getSequenceTimeExpire() + randomSec);
    end
    return result;
end
--sequence key tag  
local sequenceKey = KEYS[1];
--tenant name / id  
local tenantKey = ARGV[1];
--application name / database  
local appKey = ARGV[2];
--application module name / table name  
local module = ARGV[3];
--get generate of number  
local quantity = tonumber(ARGV[4]);
local timestamp = ARGV[5];
if (quantity < 1) then
    quantity = 1;
end
return next(sequenceKey, tenantKey, appKey, module, quantity, 8, timestamp);