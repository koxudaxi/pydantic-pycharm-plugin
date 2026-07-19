from pydantic import model_validator


class Model:
    @model_validator(mode="after")
    def validate_model<caret>
