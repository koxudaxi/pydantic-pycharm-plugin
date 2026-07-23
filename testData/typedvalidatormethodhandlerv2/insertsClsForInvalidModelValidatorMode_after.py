from pydantic import model_validator


class Model:
    @model_validator(mode=None)
    def validate_model(cls, <caret>):
