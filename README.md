# CS125 Final Project

## Overview

This project is meant to be an exploration of Web APIs in order to make a simple Android application.


## To run

Clone this repo using `git clone` and add these lines to the gradle.properties file:
```
CLARIFAI_API_KEY = '(insert Clarifai API key here)'
FOOD2FORK_API_KEY = '(insert Food2Fork API key here)'
```

## Methods

We primarily used two APIs we found online: Clarifai and Food2Fork.
- Clarifai was used to identify an image that the user inputs (from their internal storage)
- Food2Fork was used to find recipes given query keywords that the user entered

These two were used in order to create an app where the user could potentially use an image of food
to find recipes similar to the input food item and click on the recipe to be redirected to a website
that has the recipe instructions.

## Collaborators
- Alex Tamulaitis
- Shaw Kagawa
