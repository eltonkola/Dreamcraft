package com.eltonkola.dreamcraft.data.remote

import com.eltonkola.dreamcraft.data.FileManager
import com.eltonkola.dreamcraft.data.GroqRepository
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem


class FakeLocalRepositoryImpl(
    private val fileManager: FileManager
) : GroqRepository {

    override suspend fun generateGame(prompt: String, projectName: String, file: FileItem?): Result<String> {
        return try {
            val luaCode = FAKE_GAME.trimIndent()

            val filePath = fileManager.saveLuaFile(luaCode, projectName, file)

            Result.success(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    companion object {

        const val FAKE_GAME = """
-- main.lua (for Snake Game, to be loaded by your Game Maker)

function love.load()
    love.window.setTitle("Nokia Snake") -- Set the window title

    -- Snake game PREFERS a portrait aspect ratio for its LÖVE canvas.
    -- We set the LÖVE canvas to portrait dimensions.
    -- If the host "game maker" app is physically portrait, this fits well.
    -- If the host "game maker" app is physically landscape, our portrait canvas
    -- will be letterboxed within it by the calculateLayout logic.
    local preferredCanvasWidth = 480  -- Example portrait width
    local preferredCanvasHeight = 960 -- Example portrait height
    -- The {fullscreen=true} flag is important for mobile so LÖVE tries to use the whole surface.
    love.window.setMode(preferredCanvasWidth, preferredCanvasHeight, {resizable = true, vsync = true, fullscreen = true})
    print("Snake Game: Requested LÖVE canvas " .. preferredCanvasWidth .. "x" .. preferredCanvasHeight)


    -- Game constants
    GRID_WIDTH = 15
    GRID_HEIGHT = 30
    INITIAL_SPEED = 0.2
    SPEED_INCREMENT_PER_FOOD = 0.003
    MIN_SPEED = 0.05
    SWIPE_THRESHOLD = 30

    -- Visual constants
    GRID_LINE_THICKNESS = 1
    ELEMENT_PADDING = 1

    -- Colors
    BG_COLOR = {0.08, 0.15, 0.08, 1}
    GRID_LINE_COLOR = {0.2, 0.35, 0.2, 1}
    FG_COLOR = {0.6, 0.85, 0.6, 1}

    -- Initialize game variables
    gameState = "start"
    snake = {}
    direction = "right"
    nextDirection = "right"
    food = {}
    score = 0
    highScore = 0 -- loadHighScore will update this
    moveTimer = 0
    currentSpeed = INITIAL_SPEED

    touchStartX, touchStartY = nil, nil

    love.graphics.setDefaultFilter("nearest", "nearest")

    pixelFont = nil
    local success, font = pcall(love.graphics.newFont, "assets/PressStart2P-Regular.ttf", 16)
    if success then
        pixelFont = font
    else
        print("Snake Game: Custom pixel font not found. Using default.")
        pixelFont = love.graphics.newFont(16)
    end
    if pixelFont then love.graphics.setFont(pixelFont) end

    eatSound = nil
    gameOverSound = nil
    local s_eat, e_eat = pcall(love.audio.newSource, "assets/eat.ogg", "static")
    if s_eat then eatSound = e_eat else print("Snake Game: Warning: eat.ogg not found.") end
    local s_go, e_go = pcall(love.audio.newSource, "assets/gameover.ogg", "static")
    if s_go then gameOverSound = e_go else print("Snake Game: Warning: gameover.ogg not found.") end

    -- Important: calculateLayout uses love.graphics.getWidth/Height()
    -- These will reflect the dimensions of the surface LÖVE got from the host OS.
    calculateLayout()
    loadHighScore()
    resetGame() -- resetGame uses GRID_WIDTH/HEIGHT, which are fixed
                -- and spawns food based on these fixed grid coords.

    print("Snake Game: love.load() complete. Actual GFX: " .. love.graphics.getWidth() .. "x" .. love.graphics.getHeight())
end

function calculateLayout()
    -- This function now correctly uses the host app's actual screen dimensions
    -- to fit the 15x30 grid, letterboxing if necessary.
    local hostScreenWidth = love.graphics.getWidth()
    local hostScreenHeight = love.graphics.getHeight()
    print("Snake Game: calculateLayout() called with host screen " .. hostScreenWidth .. "x" .. hostScreenHeight)

    -- Calculate cell size to fit our 15x30 grid into the host screen dimensions
    local cellWidthBasedOnHost = hostScreenWidth / GRID_WIDTH
    local cellHeightBasedOnHost = hostScreenHeight / GRID_HEIGHT
    CELL_SIZE = math.floor(math.min(cellWidthBasedOnHost, cellHeightBasedOnHost))
    CELL_SIZE = math.max(1, CELL_SIZE) -- Ensure CELL_SIZE is at least 1

    -- This is the actual pixel size our 15x30 grid will occupy on the screen
    GAME_AREA_WIDTH = GRID_WIDTH * CELL_SIZE
    GAME_AREA_HEIGHT = GRID_HEIGHT * CELL_SIZE

    -- Center our game area within the host screen
    OFFSET_X = math.floor((hostScreenWidth - GAME_AREA_WIDTH) / 2)
    OFFSET_Y = math.floor((hostScreenHeight - GAME_AREA_HEIGHT) / 2)

    -- Adjust font size (optional, but good)
    if pixelFont then
        local newFontSize = math.max(8, math.floor(CELL_SIZE * 0.7))
        local currentFont = love.graphics.getFont()
        if currentFont and currentFont:getHeight() ~= newFontSize then
            local fontPath = "assets/PressStart2P-Regular.ttf"
            local successFontLoad, newLoadedFont
            if love.filesystem.getInfo(fontPath) then
                 successFontLoad, newLoadedFont = pcall(love.graphics.newFont, fontPath, newFontSize)
            else
                 successFontLoad, newLoadedFont = pcall(love.graphics.newFont, newFontSize)
            end
            if successFontLoad then
                pixelFont = newLoadedFont
                love.graphics.setFont(pixelFont)
            end
        end
    end
    print("Snake Game: CELL_SIZE="..CELL_SIZE..", GAME_AREA="..GAME_AREA_WIDTH.."x"..GAME_AREA_HEIGHT..", OFFSET="..OFFSET_X.."x"..OFFSET_Y)
end

-- love.resize is called when the host window changes size OR
-- if love.window.setMode changes the LÖVE canvas dimensions.
function love.resize(w, h)
    print("Snake Game: love.resize called with " .. w .. "x" .. h)
    -- w and h are the new dimensions of the LÖVE drawing surface.
    -- This will be the host app's screen dimensions if it resizes/rotates.
    calculateLayout()
end

function resetGame()
    -- ... (resetGame function remains the same, it works with fixed GRID_WIDTH/HEIGHT)
    snake = {
        {x = math.floor(GRID_WIDTH / 2) + 1, y = math.floor(GRID_HEIGHT / 2)},
        {x = math.floor(GRID_WIDTH / 2),     y = math.floor(GRID_HEIGHT / 2)},
        {x = math.floor(GRID_WIDTH / 2) - 1, y = math.floor(GRID_HEIGHT / 2)}
    }
    direction = "right"
    nextDirection = "right"
    score = 0
    moveTimer = 0
    currentSpeed = INITIAL_SPEED
    spawnFood()
end

function spawnFood()
    -- ... (spawnFood function remains the same)
    local onSnake
    repeat
        onSnake = false
        food.x = love.math.random(0, GRID_WIDTH - 1)
        food.y = love.math.random(0, GRID_HEIGHT - 1)
        for _, segment in ipairs(snake) do
            if food.x == segment.x and food.y == segment.y then
                onSnake = true
                break
            end
        end
    until not onSnake
end

function updateGame(dt)
    -- ... (updateGame function remains the same)
    moveTimer = moveTimer + dt
    if moveTimer >= currentSpeed then
        moveTimer = moveTimer - currentSpeed
        direction = nextDirection
        local head = {x = snake[1].x, y = snake[1].y}
        if direction == "right" then head.x = head.x + 1
        elseif direction == "left" then head.x = head.x - 1
        elseif direction == "up" then head.y = head.y - 1
        elseif direction == "down" then head.y = head.y + 1
        end
        if head.x < 0 or head.x >= GRID_WIDTH or head.y < 0 or head.y >= GRID_HEIGHT then
            gameOver()
            return
        end
        for i = 1, #snake do
            if head.x == snake[i].x and head.y == snake[i].y then
                gameOver()
                return
            end
        end
        table.insert(snake, 1, head)
        if head.x == food.x and head.y == food.y then
            score = score + 1
            if eatSound then love.audio.play(eatSound) end
            currentSpeed = math.max(MIN_SPEED, INITIAL_SPEED - score * SPEED_INCREMENT_PER_FOOD)
            spawnFood()
        else
            table.remove(snake)
        end
    end
end

function gameOver()
    -- ... (gameOver function remains the same)
    gameState = "gameover"
    if score > highScore then
        highScore = score
        saveHighScore()
    end
    if gameOverSound then love.audio.play(gameOverSound) end
end

function saveHighScore()
    -- ... (saveHighScore function remains the same)
    love.filesystem.setIdentity("NokiaSnakeGameInMaker") -- Maybe different identity if run in maker
    local success, err = love.filesystem.write("highscore.txt", tostring(highScore))
    if not success then print("Snake Game: Error saving high score: " .. tostring(err)) end
end

function loadHighScore()
    -- ... (loadHighScore function remains the same)
    love.filesystem.setIdentity("NokiaSnakeGameInMaker")
    local success, data = love.filesystem.read("highscore.txt")
    if success and tonumber(data) then highScore = tonumber(data)
    else highScore = 0
        if not success and data then print("Snake Game: Error loading high score: " .. tostring(data)) end
    end
end

function love.update(dt)
    if gameState == "playing" then
        updateGame(dt)
    end
end

function love.draw()
    -- Clear the entire LÖVE canvas (which might be letterboxed) with BG_COLOR
    -- OR, clear with a neutral color and then draw the game area with BG_COLOR.
    -- For simplicity, let's clear with BG_COLOR. If letterboxed, the bars will be BG_COLOR.
    love.graphics.clear(BG_COLOR[1], BG_COLOR[2], BG_COLOR[3], BG_COLOR[4])

    -- The rest of the drawing happens within the calculated GAME_AREA_WIDTH/HEIGHT
    -- and is offset by OFFSET_X/Y.

    -- 1. Fill actual game area background (optional if clear already did it)
    -- love.graphics.setColor(BG_COLOR[1], BG_COLOR[2], BG_COLOR[3], BG_COLOR[4])
    -- love.graphics.rectangle("fill", OFFSET_X, OFFSET_Y, GAME_AREA_WIDTH, GAME_AREA_HEIGHT)

    -- 2. Draw Grid Lines
    love.graphics.setColor(GRID_LINE_COLOR[1], GRID_LINE_COLOR[2], GRID_LINE_COLOR[3], GRID_LINE_COLOR[4])
    love.graphics.setLineWidth(GRID_LINE_THICKNESS)
    for i = 0, GRID_WIDTH do
        local x = OFFSET_X + i * CELL_SIZE
        love.graphics.line(x, OFFSET_Y, x, OFFSET_Y + GAME_AREA_HEIGHT)
    end
    for i = 0, GRID_HEIGHT do
        local y = OFFSET_Y + i * CELL_SIZE
        love.graphics.line(OFFSET_X, y, OFFSET_X + GAME_AREA_WIDTH, y)
    end
    love.graphics.setLineWidth(1)

    -- 3. Draw Snake and Food
    love.graphics.setColor(FG_COLOR[1], FG_COLOR[2], FG_COLOR[3], FG_COLOR[4])
    local totalPaddingPerDimension = 2 * (GRID_LINE_THICKNESS + ELEMENT_PADDING)
    local elementDrawSize = math.max(1, CELL_SIZE - totalPaddingPerDimension)
    local elementOffsetInCell = GRID_LINE_THICKNESS + ELEMENT_PADDING

    if gameState ~= "start" then
        if snake and #snake > 0 then
            for _, segment in ipairs(snake) do
                love.graphics.rectangle("fill",
                    OFFSET_X + segment.x * CELL_SIZE + elementOffsetInCell,
                    OFFSET_Y + segment.y * CELL_SIZE + elementOffsetInCell,
                    elementDrawSize, elementDrawSize)
            end
        end
        if food and food.x then
            love.graphics.rectangle("fill",
                OFFSET_X + food.x * CELL_SIZE + elementOffsetInCell,
                OFFSET_Y + food.y * CELL_SIZE + elementOffsetInCell,
                elementDrawSize, elementDrawSize)
        end
    end

    -- 4. Draw UI (Score, Messages) - positioned relative to the game area
    love.graphics.setColor(FG_COLOR[1], FG_COLOR[2], FG_COLOR[3], FG_COLOR[4])
    local scoreText = "Score: " .. score
    local highScoreText = "Hi: " .. highScore
    local fontHeight = 16
    if pixelFont then fontHeight = pixelFont:getHeight() end

    love.graphics.print(scoreText, OFFSET_X + 5, OFFSET_Y + 5)
    if pixelFont then
        love.graphics.print(highScoreText,
            OFFSET_X + GAME_AREA_WIDTH - pixelFont:getWidth(highScoreText) - 5,
            OFFSET_Y + 5)
    else
        love.graphics.print(highScoreText,
            OFFSET_X + GAME_AREA_WIDTH - (string.len(highScoreText) * (fontHeight*0.5)) - 5,
            OFFSET_Y + 5)
    end

    -- Centering text messages within the actual game area
    if gameState == "start" then
        local text = "Tap or Press Enter\nto Start"
        if pixelFont then
            love.graphics.printf(text,
                OFFSET_X, OFFSET_Y + (GAME_AREA_HEIGHT / 2) - (fontHeight * 1.5),
                GAME_AREA_WIDTH, "center")
        else
             love.graphics.print("Tap to Start", OFFSET_X + GAME_AREA_WIDTH / 2 - 40, OFFSET_Y + GAME_AREA_HEIGHT / 2 - 10)
        end
    elseif gameState == "gameover" then
        local text1 = "Game Over!"
        local text2 = "Score: " .. score
        local text3 = "Tap or Press Enter\nto Restart"
        if pixelFont then
            love.graphics.printf(text1, OFFSET_X, OFFSET_Y + (GAME_AREA_HEIGHT / 2) - (fontHeight * 2.5), GAME_AREA_WIDTH, "center")
            love.graphics.printf(text2, OFFSET_X, OFFSET_Y + (GAME_AREA_HEIGHT / 2) - (fontHeight * 1.0), GAME_AREA_WIDTH, "center")
            love.graphics.printf(text3, OFFSET_X, OFFSET_Y + (GAME_AREA_HEIGHT / 2) + (fontHeight * 0.5), GAME_AREA_WIDTH, "center")
        else
            love.graphics.print(text1, OFFSET_X + GAME_AREA_WIDTH/2 - 40, OFFSET_Y + GAME_AREA_HEIGHT / 2 - 40)
            love.graphics.print(text2, OFFSET_X + GAME_AREA_WIDTH/2 - 30, OFFSET_Y + GAME_AREA_HEIGHT / 2 - 20)
            love.graphics.print(text3, OFFSET_X + GAME_AREA_WIDTH/2 - 60, OFFSET_Y + GAME_AREA_HEIGHT / 2)
        end
    end
end


function handleInput(newDir)
    -- ... (handleInput function remains the same)
    if gameState == "playing" then
        if (newDir == "up" and direction ~= "down") or
           (newDir == "down" and direction ~= "up") or
           (newDir == "left" and direction ~= "right") or
           (newDir == "right" and direction ~= "left") then
            nextDirection = newDir
        end
    elseif gameState == "start" or gameState == "gameover" then
        resetGame()
        gameState = "playing"
    end
end

function love.keypressed(key)
    -- ... (love.keypressed function remains the same)
    if key == "escape" then love.event.quit()
    elseif (key == "return" or key == "kpenter") then
        if gameState == "start" or gameState == "gameover" then handleInput(nil) end
    elseif gameState == "playing" then
        if key == "up" or key == "w" then handleInput("up")
        elseif key == "down" or key == "s" then handleInput("down")
        elseif key == "left" or key == "a" then handleInput("left")
        elseif key == "right" or key == "d" then handleInput("right") end
    end
end

function love.touchpressed(id, x, y, dx, dy, pressure)
    -- ... (love.touchpressed function remains the same)
    if gameState == "playing" then touchStartX, touchStartY = x, y
    else handleInput(nil) end
end

function love.touchreleased(id, x, y, dx, dy, pressure)
    -- ... (love.touchreleased function remains the same)
    if gameState == "playing" and touchStartX then
        local diffX = x - touchStartX; local diffY = y - touchStartY
        if math.abs(diffX) > SWIPE_THRESHOLD or math.abs(diffY) > SWIPE_THRESHOLD then
            if math.abs(diffX) > math.abs(diffY) then
                if diffX > 0 then handleInput("right") else handleInput("left") end
            else
                if diffY > 0 then handleInput("down") else handleInput("up") end
            end
        end
        touchStartX, touchStartY = nil, nil
    end
end

function love.mousepressed(x, y, button, istouch, presses)
    -- ... (love.mousepressed function remains the same, useful for desktop testing via maker)
    if istouch then love.touchpressed(button, x, y, 0, 0, 1)
    elseif button == 1 then love.touchpressed("mouse"..button, x, y, 0,0,1) end
end

function love.mousereleased(x, y, button, istouch, presses)
    -- ... (love.mousereleased function remains the same)
    if istouch then love.touchreleased(button, x, y, 0, 0, 1)
    elseif button == 1 then love.touchreleased("mouse"..button, x,y,0,0,1) end
end            
        """
    }
}