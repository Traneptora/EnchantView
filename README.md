# EnchantView #

EnchantView is a very advanced mod that automatically switches your tools and weapons.

See [http://is.gd/ThebombzensMods#EnchantView](http://is.gd/ThebombzensMods#EnchantView) for details.

Note: If you want to contribute, feel free! Just send me a pull request and I'll review it before adding it.

## Compiling ##

First, you need to clone [ThebombzenAPI](https://github.com/thebombzen/ThebombzenAPI) to the same directory that you cloned EnchantView. i.e. you should see ThebombzenAPI/ and EnchantView/ in the same directory.

Then navigate to EnchantView and run:

	$ ./build.sh

This will create the directory "build" which and all build supplies inside of it, and should create a finished EnchantView jar file upon completion.

On Windows? Sorry, you're on your own. I don't know how to write CMD Batch files. 

## Eclipse ##

Once you've run the buildscript at least once, you can go to Eclipse and select File -> Import -> Existing Projects to Workspace, and select EnchantView as the root project directory. If you have the Git plugin it should recognize EnchantView as a git respository.

## Releases ##

The releases in the upper-right contain intermediary releases that don't bump the version number. This is to publish hotfixes without reminding everyone to update.


