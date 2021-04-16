from pathlib import Path
from typing import Any, Optional, Union

from .main import BaseModel

env_file_sentinel = str(object())

class BaseSettings(BaseModel):
    def __init__(
            __pydantic_self__,
            _env_file: Union[Path, str, None] = env_file_sentinel,
            _env_file_encoding: Optional[str] = None,
            _secrets_dir: Union[Path, str, None] = None,
            **values: Any,
    ) -> None:
        pass
