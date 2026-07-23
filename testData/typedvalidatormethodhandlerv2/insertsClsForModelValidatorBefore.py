from pydantic import model_validator


class Model:
    @model_validator(mode="before")
    def validate_model<caret>
