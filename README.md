# structure-item-mod
A Forge mod that adds an item that places structures

## Example Usage

```
/give User structure_item:item{offset: [I; 0, 5, 0],structure: "structure:mine",allowedOn: "minecraft:stone",blacklist:["minecraft:bedrock"]} 1
```  
and tada, you have a item capable of placing structures on rightclick.  
If the clicked block is Stone and neither Damond blocks nor Bedrock is in the way, it will place the structure "structure:mine" moved up by 5 blocks. (The low coordinate corner will start 5 blocks above the block you would have placed if this item were a block). Blocks (non structurevoids) delete intersecting entities.  
If you leave out structure, it won't work.  
If you leave out offset, it will place the structure at the block you clicked at, expanding in your view direction, up and to both sides. if you look up or down, it'll place the structure centered above or below the block you clicked at.  
If you leave out blacklist it will replace anything.  
If you leave out allowedOn it will allow "placement" on any block.

Any creator errors will be send in chat, any user errors will get a massage on the center of the screen.

You'd probably want to change the model of the item (currently = stick) and to make the language file make sense, if you use this mod.
