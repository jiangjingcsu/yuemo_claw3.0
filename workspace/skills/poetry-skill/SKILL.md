---
name: poetry-skill
description: Professional poetry creation assistant. Create various types and styles of poetry including Tang poetry, Song lyrics, modern poetry, and more.
---

# Poetry Skill

## Description

Professional poetry creation assistant, capable of creating various types and styles of poetry works.

## Purpose

- Create Tang poetry (five-character, seven-character, quatrains, regulated verses)
- Create Song lyrics (various cipai)
- Create modern poetry
- Poetry appreciation and critique
- Create acrostic and tail-hidden poetry

## Usage

### Poetry Types

- `tang`: Tang poetry (classical Chinese poetry)
  - `wujue`: Five-character quatrain
  - `qijue`: Seven-character quatrain
  - `wulv`: Five-character regulated verse
  - `qilv`: Seven-character regulated verse
- `song`: Song lyrics (classical Chinese song lyrics)
  - Specify cipai (tone pattern)
- `modern`: Modern poetry
  - `lyric`: Lyric poetry
  - `narrative`: Narrative poetry
  - `philosophical`: Philosophical poetry
- `acrostic`: Acrostic poetry (hidden characters)

### Parameters

- `type`: Poetry type (tang, song, modern, acrostic)
- `theme`: Poetry theme/subject
- `emotion`: Emotional tone (happy, sad, nostalgic, etc.)
- `form`: Specific form (for tang/song types)
- `hidden_text`: Hidden text (for acrostic type)

### Example Usage

```json
{
  "type": "tang",
  "form": "qijue",
  "theme": "spring",
  "emotion": "joyful"
}
```

```json
{
  "type": "acrostic",
  "hidden_text": "生日快乐",
  "theme": "birthday celebration",
  "form": "modern"
}
```

## Features

### Creation Principles

1. **Beautiful imagery**: Poetry should have visual appeal and emotional depth
2. **Refined wording**: Beautiful language, every word counts
3. **Harmonious rhyme**: Classical poetry should follow tonal patterns and rhymes
4. **Genuine emotion**: Poetry should express true feelings

### Creation Process

1. **Understand theme**: Clarify the emotion or theme the user wants to express
2. **Choose form**: Select appropriate poetry form based on theme and emotion
3. **Conceive imagery**: Imagine the scenes and settings of the poetry
4. **Craft wording**: Choose beautiful words and expressions
5. **Polish and refine**: Revise repeatedly, striving for perfection

## Skills

- Tang poetry creation
- Song lyrics creation
- Modern poetry creation
- Acrostic poetry creation
- Poetry appreciation and critique

## Notes

- Can create both classical and modern poetry
- Respects traditional poetic forms and techniques
- Can create poetry for various occasions and themes
