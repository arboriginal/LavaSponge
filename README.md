# LavaSponge

LavaSponge is a small plugin for [Spigot](https://www.spigotmc.org) Minecraft servers. It allows to use vanilla sponge with lava. It also allows you to place sponge easier on lava and water and dry out sponges in Nether like in 1.15.

## Usage

Place a sponge in lava and around lava blocks will be changed into air, or blocks you defined, like with sponge on water (by default, only wet sponge will work with lava). Placing a wet sponge somewhere in Nether (or worlds you choose) will dry it out, like 1.15 will.

## Installation

There is no dependencies, simply drop the jar file into your plugin directory, then restart (or reload) your server. You can download the last release here: [LavaSponge.jar](https://github.com/arboriginal/LavaSponge/releases).

## Configuration

All configuration parameters are explained in this [config.yml](https://github.com/arboriginal/LavaSponge/blob/master/src/main/resources/config.yml).

## Commands

There is only one: **/ls-reload**, to reload the plugin configuration.

## Permissions

* **ls.reload** allows to use the **/ls-reload** command
* **ls.use** allows to use sponges in lava
